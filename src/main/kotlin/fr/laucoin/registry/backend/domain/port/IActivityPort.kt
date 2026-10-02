package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.enumeration.ActivitySortFieldEnum
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.SortModel
import java.time.LocalDate
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Persistence port for [ActivityModel]: CRUD, paginated/filtered search, dashboard/picker queries, and
 * the retention lookup used by the purge job. Implemented by the jOOQ Postgres adapter; the domain
 * only depends on this contract, never on jOOQ directly.
 */
interface IActivityPort {
	fun findAllByCreatorId(userId: UUID): Flux<ActivityModel>
	fun findById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<ActivityModel>
	fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ActivitySearchParamModel,
		sortFields: List<SortModel<ActivitySortFieldEnum>> = emptyList(),
	): Mono<PageModel<ActivityModel>>

	fun findAllByIds(projectId: UUID, ids: List<UUID>, isVisible: Boolean?): Flux<ActivityModel>
	fun findWithLimit(limit: Int, projectId: UUID, searchParams: ActivitySearchParamModel): Flux<ActivityModel>
	fun findUnusedSince(dateThreshold: LocalDate): Flux<UUID>
	fun create(element: ActivityModel): Mono<ActivityModel>
	fun update(element: ActivityModel): Mono<ActivityModel>
	fun deleteById(id: UUID): Mono<Unit>
}