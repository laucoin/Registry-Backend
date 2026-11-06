package fr.laucoin.registry.backend.domain.service.core.impl

import reactor.kotlin.core.util.function.component5
import reactor.kotlin.core.util.function.component4
import reactor.kotlin.core.util.function.component3
import reactor.kotlin.core.util.function.component2
import reactor.kotlin.core.util.function.component1
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.GUEST
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.extension.DateExt.isMajor
import fr.laucoin.registry.backend.domain.helper.MovementPersistenceHelper
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.ProjectStatusModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.model.VehicleStatusModel
import fr.laucoin.registry.backend.domain.port.ICommunicationPort
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.port.IVehiclePort
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.core.IMovementQueryService
import java.time.Clock
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Mono.zip

/**
 * Objectif : Lit les mouvements, leur contenu, leurs communications et les statuts de présence des participants et véhicules.
 * Périmètre : Pagination des mouvements, lecture par identifiant, comptage des présents et absents.
 * Limites : Ne crée ni ne modifie aucun mouvement.
 */
@Service
class MovementQueryService(
	private val port: IMovementPort,
	private val communicationPort: ICommunicationPort,
	private val participantPort: IParticipantPort,
	private val vehiclePort: IVehiclePort,
	private val persistence: MovementPersistenceHelper,
	clock: Clock,
) : IMovementQueryService, GenericService(clock) {
	override fun findMovementsPage(
		currentUser: CurrentUserModel,
		projectId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
	): Mono<PageModel<MovementModel>> {
		return Mono.defer { port.findPage(projectId, pageable, searchParams) }
			.requireActivityOption(currentUser, projectId, searchParams.isActivityRequested())
	}

	override fun findCurrentMovementsPage(
		currentUser: CurrentUserModel,
		projectId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel
	): Mono<PageModel<MovementModel>> {
		return Mono.defer { port.findCurrentPage(projectId, pageable, searchParams) }
			.requireActivityOption(currentUser, projectId, searchParams.isActivityRequested())
	}

	private fun MovementSearchParamModel.isActivityRequested() = hasActivity == true || activityId != null

	override fun findMovementsContent(
		projectId: UUID,
		movementIds: List<UUID>
	): Flux<Pair<UUID, List<MovementContentModel>>> {
		return port.findContent(projectId, movementIds)
	}

	override fun findCurrentMovementsContent(
		projectId: UUID,
		movementIds: List<UUID>
	): Flux<Pair<UUID, List<MovementContentModel>>> {
		return port.findCurrentContent(projectId, movementIds)
	}

	override fun findMovementCommunicationsPage(
		projectId: UUID,
		id: UUID,
		pageable: PageableModel,
		searchParams: CommunicationSearchParamModel
	): Mono<PageModel<CommunicationModel>> {
		return communicationPort.findPageByMovementId(projectId, id, pageable, searchParams)
	}

	override fun findParticipantsStatus(projectId: UUID): Mono<ProjectStatusModel> {
		return zip(
			countParticipants(projectId, isMajor = true, type = REGISTERED, presence = PresenceStatusEnum.IN),
			countParticipants(projectId, isMajor = true, type = REGISTERED, presence = PresenceStatusEnum.OUT),
			countParticipants(projectId, isMajor = false, type = REGISTERED, presence = PresenceStatusEnum.IN),
			countParticipants(projectId, isMajor = false, type = REGISTERED, presence = PresenceStatusEnum.OUT),
			countParticipants(projectId, isMajor = null, type = GUEST, presence = PresenceStatusEnum.IN),
		)
			.map { (presentAdult, absentAdult, presentMinor, absentMinor, guestPresent) ->
				toProjectStatus(presentAdult, absentAdult, presentMinor, absentMinor, guestPresent)
			}
	}

	private fun toProjectStatus(
		presentAdult: Long,
		absentAdult: Long,
		presentMinor: Long,
		absentMinor: Long,
		guestPresent: Long,
	) = ProjectStatusModel(
		registered = ProjectStatusModel.ParticipantStatusModel(presentMinor, presentAdult, absentMinor, absentAdult),
		guests = guestPresent,
		lastRefresh = now(),
	)

	private fun countParticipants(
		projectId: UUID,
		isMajor: Boolean?,
		type: ParticipantTypeEnum,
		presence: PresenceStatusEnum,
	): Mono<Long> {
		return participantPort.countAll(
			projectId,
			ParticipantSearchParamModel(
				query = null,
				isMajor = isMajor,
				type = type,
				presence = presence,
				isVisible = true,
				at = null,
			),
		)
	}

	override fun findVehiclesStatus(projectId: UUID): Mono<VehicleStatusModel> {
		return zip(
			countVehicles(projectId, PresenceStatusEnum.IN),
			countVehicles(projectId, PresenceStatusEnum.OUT),
		)
			.map { (vehiclePresent, vehicleAbsent) -> VehicleStatusModel(present = vehiclePresent, absent = vehicleAbsent, lastRefresh = now()) }
	}

	private fun countVehicles(projectId: UUID, status: PresenceStatusEnum): Mono<Long> {
		return vehiclePort.countAll(
			projectId,
			VehicleSearchParamModel(query = null, isVisible = true, status = status, at = null),
		)
	}

	override fun findMovementById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<MovementModel> {
		return persistence.findMovement(projectId, id, isVisible)
	}
}
