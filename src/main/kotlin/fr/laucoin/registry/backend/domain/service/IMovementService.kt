package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PlannedMovementModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ProjectStatusModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.model.VehicleStatusModel
import java.time.LocalDate
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.function.Tuple2

/**
 * Purpose: Use-case contract of the movement.
 * Scope: Declares what the controllers and other services can ask for movement.
 * Limits: Contract only; the business rules live in the implementation.
 */
interface IMovementService {
	fun findMovementsPage(
		currentUser: CurrentUserModel,
		projectId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
	): Mono<PageModel<MovementModel>>

	fun findCurrentMovementsPage(
		currentUser: CurrentUserModel,
		projectId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
	): Mono<PageModel<MovementModel>>

	fun findMovementsContent(
		projectId: UUID,
		movementIds: List<UUID>,
	): Flux<Pair<UUID, List<MovementContentModel>>>

	fun findCurrentMovementsContent(
		projectId: UUID,
		movementIds: List<UUID>,
	): Flux<Pair<UUID, List<MovementContentModel>>>

	fun findMovementById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<MovementModel>
	fun searchParticipantsAndGroupsByText(
		projectId: UUID,
		type: ParticipantTypeEnum,
		query: String?
	): Mono<Tuple2<List<ParticipantModel>, List<GroupModel>>>

	fun searchVehiclesByText(projectId: UUID, query: String?): Flux<VehicleModel>
	fun searchReasonsByText(
		contentType: ParticipantTypeEnum,
		type: MovementTypeEnum,
	): Flux<MovementReasonEnum>

	fun searchActivitiesByText(
		projectId: UUID,
		contentType: ParticipantTypeEnum,
		query: String?
	): Flux<ActivityModel>

	fun findMovementCommunicationsPage(
		projectId: UUID,
		id: UUID,
		pageable: PageableModel,
		searchParams: CommunicationSearchParamModel,
	): Mono<PageModel<CommunicationModel>>

	fun findPlannedMovements(projectId: UUID, type: MovementTypeEnum, date: LocalDate?): Flux<PlannedMovementModel>

	fun findParticipantsStatus(projectId: UUID): Mono<ProjectStatusModel>

	fun findVehiclesStatus(projectId: UUID): Mono<VehicleStatusModel>

	fun createMovement(
		currentUser: CurrentUserModel,
		movement: MovementModel,
		newGuests: List<ParticipantModel> = emptyList()
	): Mono<MovementModel>

	fun updateMovementById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		movement: MovementModel,
		newGuests: List<ParticipantModel> = emptyList()
	): Mono<MovementModel>

	fun disableMovementById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<MovementModel>
	fun enableMovementById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<MovementModel>
	fun deleteMovementById(projectId: UUID, id: UUID): Mono<Unit>
	fun purgeMovementsIfNecessary(dateThreshold: LocalDate, dryRun: Boolean): Flux<UUID>
}
