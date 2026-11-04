package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.GUEST
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.extension.DateExt.isMajor
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
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
import fr.laucoin.registry.backend.domain.service.IMovementQueryService
import java.time.Clock
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Mono.zip
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import reactor.kotlin.core.util.function.component3
import reactor.kotlin.core.util.function.component4
import reactor.kotlin.core.util.function.component5
import reactor.util.function.Tuple5

/**
 * Objectif : Consulte les mouvements : pages, contenu, détail, communications liées et statuts de présence.
 * Périmètre : Lecture seule des mouvements, avec la garde de l'option d'activité sur les filtres d'activité.
 * Limites : Ne modifie rien ; la recherche textuelle et les mouvements planifiés relèvent de MovementSearchService.
 */
@Service
class MovementQueryService(
	private val port: IMovementPort,
	private val participantPort: IParticipantPort,
	private val vehiclePort: IVehiclePort,
	private val communicationPort: ICommunicationPort,
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
	): Flux<Pair<UUID, List<MovementContentModel>>> =
		port.findContent(projectId, movementIds)

	override fun findCurrentMovementsContent(
		projectId: UUID,
		movementIds: List<UUID>
	): Flux<Pair<UUID, List<MovementContentModel>>> =
		port.findCurrentContent(projectId, movementIds)

	override fun findMovementById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<MovementModel> =
		port.findById(projectId, id, isVisible)
			.notFoundIfEmpty(id)

	override fun findMovementCommunicationsPage(
		projectId: UUID,
		id: UUID,
		pageable: PageableModel,
		searchParams: CommunicationSearchParamModel
	): Mono<PageModel<CommunicationModel>> =
		communicationPort.findPageByMovementId(projectId, id, pageable, searchParams)

	override fun findParticipantsStatus(projectId: UUID): Mono<ProjectStatusModel> =
		zip(
			countParticipants(projectId, isMajor = true, type = REGISTERED, presence = PresenceStatusEnum.IN),
			countParticipants(projectId, isMajor = true, type = REGISTERED, presence = PresenceStatusEnum.OUT),
			countParticipants(projectId, isMajor = false, type = REGISTERED, presence = PresenceStatusEnum.IN),
			countParticipants(projectId, isMajor = false, type = REGISTERED, presence = PresenceStatusEnum.OUT),
			countParticipants(projectId, isMajor = null, type = GUEST, presence = PresenceStatusEnum.IN),
		)
			.map(::toProjectStatus)

	private fun toProjectStatus(counts: Tuple5<Long, Long, Long, Long, Long>): ProjectStatusModel {
		val registeredPresentAdult = counts.t1
		val registeredAbsentAdult = counts.t2
		val registeredPresentMinor = counts.t3
		val registeredAbsentMinor = counts.t4
		val guestPresent = counts.t5
		return ProjectStatusModel(
			registered = ProjectStatusModel.ParticipantStatusModel(
				registeredPresentMinor,
				registeredPresentAdult,
				registeredAbsentMinor,
				registeredAbsentAdult,
			),
			guests = guestPresent,
			lastRefresh = now(),
		)
	}

	private fun countParticipants(
		projectId: UUID,
		isMajor: Boolean?,
		type: ParticipantTypeEnum,
		presence: PresenceStatusEnum,
	): Mono<Long> =
		participantPort.countAll(
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

	override fun findVehiclesStatus(projectId: UUID): Mono<VehicleStatusModel> {
		return zip(
			countVehicles(projectId, PresenceStatusEnum.IN),
			countVehicles(projectId, PresenceStatusEnum.OUT),
		)
			.map { (
				vehiclePresent,
				vehicleAbsent,
			) -> VehicleStatusModel(present = vehiclePresent, absent = vehicleAbsent, lastRefresh = now()) }
	}

	private fun countVehicles(projectId: UUID, status: PresenceStatusEnum): Mono<Long> {
		return vehiclePort.countAll(
			projectId,
			VehicleSearchParamModel(query = null, isVisible = true, status = status, at = null),
		)
	}
}
