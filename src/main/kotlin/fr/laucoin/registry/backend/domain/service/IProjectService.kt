package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.ProjectSortFieldEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.model.SortModel
import java.time.LocalDate
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Use-case entry point for Projects: search/read, available options metadata, schedule-overlap
 * validation, the create/update/disable/enable/delete lifecycle, and the retention purge sweep.
 * Callers go through this contract, never the [IProjectPort] directly.
 */
interface IProjectService {
	fun findProjectsPage(
		currentUser: CurrentUserModel,
		pageable: PageableModel,
		withProfile: Boolean,
		searchParams: ProjectSearchParamModel,
		sortFields: List<SortModel<ProjectSortFieldEnum>> = emptyList(),
	): Mono<PageModel<ProjectModel>>

	fun findProjectById(id: UUID, isVisible: Boolean?, currentUser: CurrentUserModel? = null): Mono<ProjectModel>

	/**
	 * The caller's ACCEPTED, still-in-progress Projects with at least one ongoing Alert, sorted by
	 * that count descending — the "Projects requiring attention" dashboard widget. Each result carries
	 * its counts and the caller's own [ProjectModel.activeProfile].
	 */
	fun findProjectsRequiringAttention(currentUser: CurrentUserModel, limit: Int): Flux<ProjectModel>

	fun validateDateTime(id: UUID, dateTime: CustomDateTimeModel?, errorCode: String): Mono<UUID>
	fun validateDateTimes(
		id: UUID,
		start: CustomDateTimeModel?,
		end: CustomDateTimeModel?,
		errorCode: String
	): Mono<UUID>

	fun createProject(currentUser: CurrentUserModel, project: ProjectModel): Mono<ProjectModel>
	fun updateProjectById(currentUser: CurrentUserModel, id: UUID, project: ProjectModel): Mono<ProjectModel>
	fun disableProjectById(currentUser: CurrentUserModel, id: UUID): Mono<ProjectModel>
	fun enableProjectById(currentUser: CurrentUserModel, id: UUID): Mono<ProjectModel>
	fun deleteProjectById(id: UUID): Mono<Unit>
	fun purgeProjectsIfNecessary(dateThreshold: LocalDate, dryRun: Boolean): Flux<UUID>
}
