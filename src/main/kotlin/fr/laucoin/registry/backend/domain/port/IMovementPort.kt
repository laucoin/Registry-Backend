package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.enumeration.MovementSortFieldEnum
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.SortModel
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

interface IMovementPort {
	fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<MovementModel>
	fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
		sort: List<SortModel<MovementSortFieldEnum>> = emptyList(),
	): Mono<PageModel<MovementModel>>

	fun findCurrentPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
		sort: List<SortModel<MovementSortFieldEnum>> = emptyList(),
	): Mono<PageModel<MovementModel>>

	fun findContent(
		projectId: UUID,
		movementIds: List<UUID>,
	): Flux<Pair<UUID, List<MovementContentModel>>>

	fun findCurrentContent(
		projectId: UUID,
		movementIds: List<UUID>,
	): Flux<Pair<UUID, List<MovementContentModel>>>

	fun findPageByParticipantId(
		projectId: UUID,
		participantId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
	): Mono<PageModel<MovementModel>>

	fun findPageByVehicleId(
		projectId: UUID,
		vehicleId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
	): Mono<PageModel<MovementModel>>

	fun findPageByActivityId(
		projectId: UUID,
		activityId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
	): Mono<PageModel<MovementModel>>

	fun findActivityWithLimit(
		limit: Int,
		projectId: UUID,
		searchParams: ActivitySearchParamModel,
	): Flux<MovementModel>

	fun findOngoingActivities(projectId: UUID, limit: Int): Flux<MovementModel>

	fun countAllByParticipantId(
		projectId: UUID,
		participantId: UUID,
		searchParams: MovementSearchParamModel
	): Mono<Long>

	fun countAllByVehicleId(projectId: UUID, vehicleId: UUID, searchParams: MovementSearchParamModel): Mono<Long>
	fun countAllByActivityId(projectId: UUID, activityId: UUID, searchParams: MovementSearchParamModel): Mono<Long>
	fun findOlderThanAndUncommentedSince(dateThreshold: LocalDate): Flux<UUID>
	fun create(element: MovementModel): Mono<MovementModel>
	fun update(element: MovementModel): Mono<MovementModel>
	fun deleteById(id: UUID): Mono<Unit>
}
