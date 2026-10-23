package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_EXPORT
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantExportModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserExportModel
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.port.IPreferencesPort
import fr.laucoin.registry.backend.domain.port.IProjectPort
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IExportService
import java.time.Clock
import java.time.ZonedDateTime
import java.util.Optional
import java.util.UUID
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Purpose: Builds the GDPR personal data exports (user and participant) for access and portability rights.
 * Scope: Export authorization rules and assembly of the export models.
 * Limits: Never loads third-party data (other participants, other authors, free-text messages); does not modify data.
 */
@Service
class ExportService(
	private val userPort: IUserPort,
	private val preferencesPort: IPreferencesPort,
	private val profilePort: IProjectProfilePort,
	private val participantPort: IParticipantPort,
	private val movementPort: IMovementPort,
	private val projectPort: IProjectPort,
	private val clock: Clock,
): IExportService, GenericService() {
	companion object {
		private const val EXPORT_PAGE_SIZE = 200
	}

	override fun exportUser(currentUser: CurrentUserModel, userId: UUID): Mono<UserExportModel> {
		log.info("User {} exports the data of the user {}", currentUser.id, userId)
		val generatedAt = ZonedDateTime.now(clock)

		return userPort.findById(userId, isVisible = null)
			.notFoundIfEmpty(userId)
			.flatMap { user ->
				Mono.zip(
					preferencesPort.findByUserId(userId, isVisible = null).map { Optional.of(it) }
						.defaultIfEmpty(Optional.empty()),
					findAllProfiles(userId).collectList(),
					findAllParticipantExports(userId, generatedAt).collectList(),
				).map { UserExportModel(generatedAt, user, it.t1.orElse(null), it.t2, it.t3) }
			}
	}

	override fun exportParticipant(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID
	): Mono<ParticipantExportModel> {
		return participantPort.findById(projectId, id, isVisible = null)
			.notFoundIfEmpty(id)
			.filter { canExportParticipant(currentUser, projectId, it) }
			.switchIfEmpty(Mono.error { forbidden(currentUser, id) })
			.doOnNext { log.info("User {} exports the data of the participant {}", currentUser.id, id) }
			.flatMap { buildParticipantExport(it, ZonedDateTime.now(clock)) }
	}

	private fun canExportParticipant(currentUser: CurrentUserModel, projectId: UUID, participant: ParticipantModel) =
		currentUser.hasAuthority(projectId, REGISTRY_PROJECT_PARTICIPANT_EXPORT) ||
			(participant.user?.id != null && participant.user?.id == currentUser.id)

	private fun forbidden(currentUser: CurrentUserModel, id: UUID): RegistryException {
		log.warn("User {} is not allowed to export the participant {}", currentUser.id, id)
		return RegistryException(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	private fun findAllParticipantExports(userId: UUID, generatedAt: ZonedDateTime) =
		participantPort.findAllByUserId(userId)
			.concatMap { participantPort.findById(it.projectId!!, it.id!!, isVisible = null) }
			.concatMap { buildParticipantExport(it, generatedAt) }

	private fun buildParticipantExport(
		participant: ParticipantModel,
		generatedAt: ZonedDateTime
	): Mono<ParticipantExportModel> {
		val projectId = participant.projectId!!

		return Mono.zip(
			projectPort.findById(projectId, isVisible = null).map { it.name ?: "" }.defaultIfEmpty(""),
			findAllMovements(projectId, participant.id!!).collectList(),
		).map { ParticipantExportModel(generatedAt, it.t1.ifEmpty { null }, participant, it.t2) }
	}

	private fun findAllMovements(projectId: UUID, participantId: UUID): Flux<MovementModel> =
		allPages {
			movementPort.findPageByParticipantId(projectId, participantId, it, MovementSearchParamModel())
		}

	private fun findAllProfiles(userId: UUID): Flux<ProjectProfileModel> =
		allPages {
			profilePort.findProjectProfilesPageByUserId(userId, it, ProjectProfileSearchParamModel(isVisible = null))
		}

	private fun <T : Any> allPages(pageNumber: Int = 0, fetch: (PageableModel) -> Mono<PageModel<T>>): Flux<T> =
		fetch(PageableModel(pageNumber * EXPORT_PAGE_SIZE, EXPORT_PAGE_SIZE))
			.flatMapMany { page ->
				val next = if (pageNumber + 1 < page.totalPages) allPages(pageNumber + 1, fetch) else Flux.empty()
				Flux.fromIterable(page.content).concatWith(next)
			}
}
