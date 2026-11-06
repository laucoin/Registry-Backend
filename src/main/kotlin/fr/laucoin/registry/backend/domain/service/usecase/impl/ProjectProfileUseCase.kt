package fr.laucoin.registry.backend.domain.service.usecase.impl

import reactor.core.publisher.Flux
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum.PROJECT
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.port.IRowLockPort
import fr.laucoin.registry.backend.domain.service.core.IProjectProfileService
import fr.laucoin.registry.backend.domain.service.core.IUserService
import fr.laucoin.registry.backend.domain.service.usecase.IProjectProfileUseCase
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono

/**
 * Objectif : Invite des utilisateurs à un projet à partir de leurs adresses e-mail, dans une même transaction.
 * Périmètre : Verrouille le projet, résout les utilisateurs (en créant les inconnus en attente) puis crée leurs profils.
 * Limites : Ne porte aucune règle propre aux utilisateurs ni aux profils ; elles restent dans leurs services.
 */
@Service
class ProjectProfileUseCase(
	private val profileService: IProjectProfileService,
	private val userService: IUserService,
	private val rowLockPort: IRowLockPort,
	private val transactionalOperator: TransactionalOperator,
) : IProjectProfileUseCase {
	override fun createProjectProfiles(
		currentUser: CurrentUserModel,
		projectId: UUID,
		profiles: List<ProjectProfileModel>,
	): Mono<Pair<List<String>, List<String>>> {
		val emails = profiles.map { it.user!!.email!! }

		return rowLockPort.lock(PROJECT, projectId)
			.then(Mono.defer { userService.resolveUsersByEmail(emails) })
			.flatMap { users -> profileService.createProjectProfiles(currentUser, projectId, profiles, users) }
			.`as`(transactionalOperator::transactional)
	}

	override fun findProjectProfilesPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ProjectProfileSearchParamModel,
	): Mono<PageModel<ProjectProfileModel>> {
		return profileService.findProjectProfilesPage(projectId, pageable, searchParams)
	}

	override fun findProjectProfileById(
		projectId: UUID,
		id: UUID,
		isVisible: Boolean?,
	): Mono<ProjectProfileModel> {
		return profileService.findProjectProfileById(projectId, id, isVisible)
	}

	override fun searchUsers(query: String?): Flux<UserModel> {
		return profileService.searchUsers(query)
	}

	override fun getAssignableProjectRoles(currentUser: CurrentUserModel, projectId: UUID): Flux<String> {
		return profileService.getAssignableProjectRoles(currentUser, projectId)
	}

	override fun updateProjectProfileById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		profile: ProjectProfileModel,
	): Mono<ProjectProfileModel> {
		return profileService.updateProjectProfileById(currentUser, projectId, id, profile)
	}

	override fun blockProjectProfileById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<ProjectProfileModel> {
		return profileService.blockProjectProfileById(currentUser, projectId, id)
	}

	override fun unblockProjectProfileById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<ProjectProfileModel> {
		return profileService.unblockProjectProfileById(currentUser, projectId, id)
	}

	override fun deleteProjectProfileById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Unit> {
		return profileService.deleteProjectProfileById(currentUser, projectId, id)
	}
}
