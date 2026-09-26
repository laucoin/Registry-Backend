package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProjectProfileSortFieldEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.GenericModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.model.UserProjectProfileModel
import java.util.UUID
import reactor.core.publisher.Mono

/**
 * Use-case entry point for the caller's own Project Profiles: paginated listing, the
 * last-administrator safeguard check, granting a Profile when creating a Project or a temporary
 * support Profile, accepting/rejecting an invitation, toggling the favorite flag, and self-deletion.
 * Complements [IProjectProfileService], which covers a Project administrator's view of its members.
 */
interface IUserProjectProfileService {
	fun findProjectProfilesPage(
		userId: UUID,
		pageable: PageableModel,
		searchParams: ProjectProfileSearchParamModel,
		sortFields: List<SortModel<ProjectProfileSortFieldEnum>> = emptyList(),
		includeCounts: Boolean = false,
	): Mono<PageModel<UserProjectProfileModel>>

	fun <T: GenericModel> validateNotLastProjectRoleLevel0(
		userId: UUID,
		projectId: UUID?,
		result: T,
		error: String
	): Mono<T>

	fun createUserProjectProfileFromProject(
		currentUser: CurrentUserModel,
		project: ProjectModel
	): Mono<ProjectProfileModel>

	fun updateUserProjectProfileStatusById(
		currentUser: CurrentUserModel,
		id: UUID,
		status: ProfileStatusEnum
	): Mono<UserProjectProfileModel>

	fun createSupportProjectProfile(currentUser: CurrentUserModel, projectId: UUID): Mono<UserProjectProfileModel>
	fun toggleFavoriteProjectProfileById(currentUser: CurrentUserModel, id: UUID): Mono<UserProjectProfileModel>
	fun deleteUserProjectProfileById(currentUser: CurrentUserModel, id: UUID): Mono<Unit>
}
