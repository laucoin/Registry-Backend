package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.enumeration.ProjectSortFieldEnum
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.model.SortModel
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Persistence port for [ProjectModel]: CRUD, paginated/filtered search (optionally restricted to a
 * caller's own Profiles), schedule-overlap validation, and the retention lookup used by the purge
 * job. Implemented by the jOOQ Postgres adapter.
 */
interface IProjectPort {
	fun findById(id: UUID, isVisible: Boolean?): Mono<ProjectModel>
	fun findPage(
		userId: UUID,
		pageable: PageableModel,
		searchParams: ProjectSearchParamModel,
		sortFields: List<SortModel<ProjectSortFieldEnum>> = emptyList(),
	): Mono<PageModel<ProjectModel>>

	fun findPage(
		userId: UUID,
		projectIds: List<UUID>,
		pageable: PageableModel,
		searchParams: ProjectSearchParamModel,
		sortFields: List<SortModel<ProjectSortFieldEnum>> = emptyList(),
	): Mono<PageModel<ProjectModel>>

	fun validDateTime(id: UUID, begin: ZonedDateTime?, end: ZonedDateTime?): Mono<Boolean>
	fun findProjectsEligibleForPurge(dateThreshold: LocalDate): Flux<UUID>
	fun create(element: ProjectModel): Mono<ProjectModel>
	fun update(element: ProjectModel): Mono<ProjectModel>
	fun deleteById(id: UUID): Mono<Unit>
}
