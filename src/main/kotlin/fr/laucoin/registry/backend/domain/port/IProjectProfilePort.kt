package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProjectProfileSortFieldEnum
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileRoleCountModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileRoleModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.model.UserProjectProfileModel
import java.time.ZonedDateTime
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Persistence port for [ProjectProfileModel]: CRUD (single and bulk), paginated/filtered search
 * scoped to a User or a Project, role/administrator lookups (including the last-level-0-admin
 * safeguard queries) and invitation-conflict checks. Implemented by the jOOQ Postgres adapter.
 */
interface IProjectProfilePort {
	fun findAllByCreatorId(userId: UUID): Flux<ProjectProfileModel>
	fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<ProjectProfileModel>
	fun findProjectProfilesPageByUserId(
		userId: UUID,
		pageable: PageableModel,
		searchParams: ProjectProfileSearchParamModel,
		sortFields: List<SortModel<ProjectProfileSortFieldEnum>> = emptyList(),
		includeCounts: Boolean = false,
	): Mono<PageModel<ProjectProfileModel>>

	fun findProjectProfilesPageByProjectId(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ProjectProfileSearchParamModel,
		sortFields: List<SortModel<ProjectProfileSortFieldEnum>> = emptyList(),
	): Mono<PageModel<ProjectProfileModel>>

	/**
	 * Same as [findProjectProfilesPageByUserId], decorated with each Profile's full Project — for
	 * [fr.laucoin.registry.backend.domain.service.IUserProjectProfileService], whose endpoints aren't
	 * scoped under a Project id.
	 */
	fun findUserProjectProfilesPageByUserId(
		userId: UUID,
		pageable: PageableModel,
		searchParams: ProjectProfileSearchParamModel,
		sortFields: List<SortModel<ProjectProfileSortFieldEnum>> = emptyList(),
		includeCounts: Boolean = false,
	): Mono<PageModel<UserProjectProfileModel>>

	/**
	 * The caller's ACCEPTED, still-in-progress Projects with at least one ongoing Alert, sorted by
	 * that count descending — backs the "Projects requiring attention" dashboard widget on
	 * [fr.laucoin.registry.backend.domain.service.IProjectService]. Each [ProjectModel] carries its
	 * counts and the caller's own [ProjectModel.activeProfile].
	 */
	fun findProjectsRequiringAttentionByUserId(userId: UUID, limit: Int): Flux<ProjectModel>

	/** Same as [findProjectProfileByUserIdAndId], decorated with the Profile's full Project. */
	fun findUserProjectProfileByUserIdAndId(
		userId: UUID,
		id: UUID,
		visibilitySearched: Boolean?,
	): Mono<UserProjectProfileModel>

	fun findUserIdsWithProjectProfileForProjectWithProfileExclusion(
		projectId: UUID,
		userIds: List<UUID>,
		profileIdToExclude: UUID?,
		statusSearched: List<ProfileStatusEnum> = ProfileStatusEnum.entries.toList(),
		startDateTimeSearched: ZonedDateTime? = null,
		endDateTimeSearched: ZonedDateTime? = null,
	): Flux<UUID>

	fun findProjectProfilesRolesByUserId(userId: UUID): Flux<ProjectProfileRoleModel>
	fun findOidcIdsByProjectId(projectId: UUID): Flux<UUID>
	fun findProjectProfileByUserIdAndId(userId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<ProjectProfileModel>
	fun findProjectProfileByProjectAndUserId(
		projectId: UUID,
		userId: UUID,
		searchParams: ProjectProfileSearchParamModel,
	): Mono<ProjectProfileModel>

	fun findProjectProfilesByProjectIdsAndUserId(
		projectIds: List<UUID>,
		userId: UUID,
		searchParams: ProjectProfileSearchParamModel,
	): Flux<ProjectProfileModel>

	fun findLevel0ProjectProfileRoleByUserId(
		userId: UUID,
		visibilitySearched: Boolean?
	): Flux<ProjectProfileRoleCountModel>

	fun findLevel0ProjectProfileRoleByProjectId(
		projectId: UUID,
		visibilitySearched: Boolean?
	): Flux<ProjectProfileModel>

	fun saveAll(profiles: List<ProjectProfileModel>): Flux<ProjectProfileModel>
	fun create(element: ProjectProfileModel): Mono<ProjectProfileModel>
	fun update(element: ProjectProfileModel): Mono<ProjectProfileModel>
	fun deleteById(id: UUID): Mono<Unit>
}
