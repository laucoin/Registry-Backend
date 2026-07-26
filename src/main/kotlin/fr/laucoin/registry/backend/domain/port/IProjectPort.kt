package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.enumeration.ProjectSortFieldEnum
import fr.laucoin.registry.backend.domain.model.OpenAlertProjectModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.model.SortModel
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

interface IProjectPort {
	fun findById(id: UUID, visibilitySearched: Boolean?): Mono<ProjectModel>
	fun findPage(
		pageable: PageableModel,
		searchParams: ProjectSearchParamModel,
		sort: List<SortModel<ProjectSortFieldEnum>> = emptyList(),
	): Mono<PageModel<ProjectModel>>

	fun findPage(
		projectIds: List<UUID>,
		pageable: PageableModel,
		searchParams: ProjectSearchParamModel,
		sort: List<SortModel<ProjectSortFieldEnum>> = emptyList(),
	): Mono<PageModel<ProjectModel>>

	fun findOpenAlertProjectsByUserId(userId: UUID, limit: Int): Flux<OpenAlertProjectModel>

	fun validDateTime(id: UUID, begin: ZonedDateTime?, end: ZonedDateTime?): Mono<Boolean>
	fun findProjectsEligibleForPurge(dateThreshold: LocalDate): Flux<UUID>
	fun create(element: ProjectModel): Mono<ProjectModel>
	fun update(element: ProjectModel): Mono<ProjectModel>
	fun deleteById(id: UUID): Mono<Unit>
}
