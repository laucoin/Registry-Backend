package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.UserModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Purpose: Use-case contract of the project profile.
 * Scope: Declares what the controllers and other services can ask for project profile.
 * Limits: Contract only; the business rules live in the implementation.
 */
interface IProjectProfileService {
	fun findProjectProfilesPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ProjectProfileSearchParamModel,
	): Mono<PageModel<ProjectProfileModel>>

	fun findProjectProfileById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<ProjectProfileModel>
	fun searchUsers(query: String?): Flux<UserModel>
	fun getAssignableProjectRoles(currentUser: CurrentUserModel, projectId: UUID): Flux<String>
	fun createProjectProfiles(
		currentUser: CurrentUserModel,
		projectId: UUID,
		userIds: List<UUID>,
		profiles: List<ProjectProfileModel>
	): Mono<Pair<List<UUID>, List<UUID>>>

	fun updateProjectProfileById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		profile: ProjectProfileModel
	): Mono<ProjectProfileModel>

	fun blockProjectProfileById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<ProjectProfileModel>
	fun unblockProjectProfileById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<ProjectProfileModel>
	fun deleteProjectProfileById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Unit>
}
