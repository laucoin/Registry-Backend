package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_EXPORT
import fr.laucoin.registry.backend.domain.enumeration.AuditActionEnum.EXPORT
import fr.laucoin.registry.backend.domain.enumeration.AuditOutcomeEnum
import fr.laucoin.registry.backend.domain.enumeration.AuditResourceTypeEnum
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.AuditModel
import fr.laucoin.registry.backend.domain.model.ClientContextModel
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
import fr.laucoin.registry.backend.domain.service.IAuditService
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
 * Objectif : Construit les exports de données personnelles RGPD (utilisateur et participant) pour les droits d'accès et de portabilité.
 * Périmètre : Règles d'autorisation de l'export, assemblage des modèles d'export et trace d'audit de chaque demande (réussie ou refusée).
 * Limites : Ne charge jamais de données de tiers (autres participants, autres auteurs, messages en texte libre) ; ne modifie aucune donnée.
 */
@Service
class ExportService(
	private val userPort: IUserPort,
	private val preferencesPort: IPreferencesPort,
	private val profilePort: IProjectProfilePort,
	private val participantPort: IParticipantPort,
	private val movementPort: IMovementPort,
	private val projectPort: IProjectPort,
	private val auditService: IAuditService,
	clock: Clock,
): IExportService, GenericService(clock) {
	companion object {
		private const val EXPORT_PAGE_SIZE = 200
	}

	override fun exportUser(
		currentUser: CurrentUserModel,
		userId: UUID,
		client: ClientContextModel,
	): Mono<UserExportModel> {
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
			.doOnNext { audit(currentUser, client, AuditResourceTypeEnum.USER, userId, null, AuditOutcomeEnum.SUCCESS) }
	}

	override fun exportParticipant(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		client: ClientContextModel,
	): Mono<ParticipantExportModel> {
		return participantPort.findById(projectId, id, isVisible = null)
			.notFoundIfEmpty(id)
			.filter { canExportParticipant(currentUser, projectId, it) }
			.switchIfEmpty(Mono.error { forbidden(currentUser, projectId, id, client) })
			.doOnNext { log.info("User {} exports the data of the participant {}", currentUser.id, id) }
			.flatMap { buildParticipantExport(it, ZonedDateTime.now(clock)) }
			.doOnNext { audit(currentUser, client, AuditResourceTypeEnum.PARTICIPANT, id, projectId, AuditOutcomeEnum.SUCCESS) }
	}

	private fun canExportParticipant(currentUser: CurrentUserModel, projectId: UUID, participant: ParticipantModel) =
		currentUser.hasAuthority(projectId, REGISTRY_PROJECT_PARTICIPANT_EXPORT) ||
			(participant.user?.id != null && participant.user?.id == currentUser.id)

	private fun forbidden(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		client: ClientContextModel,
	): RegistryException {
		log.warn("User {} is not allowed to export the participant {}", currentUser.id, id)
		audit(currentUser, client, AuditResourceTypeEnum.PARTICIPANT, id, projectId, AuditOutcomeEnum.FORBIDDEN)
		return RegistryException(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	private fun audit(
		currentUser: CurrentUserModel,
		client: ClientContextModel,
		resourceType: AuditResourceTypeEnum,
		resourceId: UUID,
		projectId: UUID?,
		outcome: AuditOutcomeEnum,
	) {
		auditService.record(AuditModel(EXPORT, resourceType, resourceId, projectId, currentUser.id, outcome, client, now()))
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
		fetch(PageableModel(pageNumber.toLong() * EXPORT_PAGE_SIZE, EXPORT_PAGE_SIZE))
			.flatMapMany { page ->
				val next = if (pageNumber + 1 < page.totalPages) allPages(pageNumber + 1, fetch) else Flux.empty()
				Flux.fromIterable(page.content).concatWith(next)
			}
}
