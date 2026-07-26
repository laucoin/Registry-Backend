package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.enumeration.ActivitySortFieldEnum
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.SortModel
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

interface IActivityPort {
	fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<ActivityModel>
	fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ActivitySearchParamModel,
		sort: List<SortModel<ActivitySortFieldEnum>> = emptyList(),
	): Mono<PageModel<ActivityModel>>

	fun findAllByIds(projectId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<ActivityModel>
	fun findWithLimit(limit: Int, projectId: UUID, searchParams: ActivitySearchParamModel): Flux<ActivityModel>
	fun findUnusedSince(dateThreshold: LocalDate): Flux<UUID>
	fun create(element: ActivityModel): Mono<ActivityModel>
	fun update(element: ActivityModel): Mono<ActivityModel>
	fun deleteById(id: UUID): Mono<Unit>
}