package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.helper.ExportAssemblyHelper
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_EXPORT
import fr.laucoin.registry.backend.domain.enumeration.AuditActionEnum.EXPORT
import fr.laucoin.registry.backend.domain.enumeration.AuditOutcomeEnum
import fr.laucoin.registry.backend.domain.enumeration.AuditResourceTypeEnum
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.AuditModel
import fr.laucoin.registry.backend.domain.model.ClientContextModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.ParticipantExportModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserExportModel
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.port.IPreferencesPort
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.support.IAuditService
import fr.laucoin.registry.backend.domain.service.usecase.IExportUseCase
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
class ExportUseCase(
	private val userPort: IUserPort,
	private val preferencesPort: IPreferencesPort,
	private val profilePort: IProjectProfilePort,
	private val participantPort: IParticipantPort,
	private val exports: ExportAssemblyHelper,
	private val auditService: IAuditService,
	clock: Clock,
): IExportUseCase, GenericService(clock) {
	override fun exportUser(
		currentUser: CurrentUserModel,
		userId: UUID,
		client: ClientContextModel,
	): Mono<UserExportModel> {
		log.info("User {} exports the data of the user {}", currentUser.id, userId)
		val generatedAt = ZonedDateTime.now(clock)

		return userPort.findById(userId, isVisible = null)
			.notFoundIfEmpty(userId)
			.flatMap { buildUserExport(it, generatedAt) }
			.doOnNext { audit(currentUser, client, AuditResourceTypeEnum.USER, userId, null, AuditOutcomeEnum.SUCCESS) }
	}

	private fun buildUserExport(user: UserModel, generatedAt: ZonedDateTime): Mono<UserExportModel> {
		return Mono.zip(
			preferencesPort.findByUserId(user.id!!, isVisible = null).map { Optional.of(it) }
				.defaultIfEmpty(Optional.empty()),
			findAllProfiles(user.id!!).collectList(),
			exports.findAllParticipantExports(user.id!!, generatedAt).collectList(),
		).map { UserExportModel(generatedAt, user, it.t1.orElse(null), it.t2, it.t3) }
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
			.flatMap { exports.buildParticipantExport(it, ZonedDateTime.now(clock)) }
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

	private fun findAllProfiles(userId: UUID): Flux<ProjectProfileModel> =
		exports.allPages {
			profilePort.findProjectProfilesPageByUserId(userId, it, ProjectProfileSearchParamModel(isVisible = null))
		}
}
