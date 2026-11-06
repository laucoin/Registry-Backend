package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.service.core.IProjectService
import fr.laucoin.registry.backend.domain.service.core.IUserProjectProfileService
import fr.laucoin.registry.backend.domain.service.support.IPrincipalCacheService
import fr.laucoin.registry.backend.domain.service.usecase.IProjectUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono

/**
 * Objectif : Crée un projet et le profil d'administrateur de son créateur dans une même transaction.
 * Périmètre : Enchaîne la création du projet, celle du profil initial et le rafraîchissement du principal mis en cache.
 * Limites : Ne porte aucune règle propre au projet ni au profil ; elles restent dans leurs services.
 */
@Service
class ProjectUseCase(
	private val projectService: IProjectService,
	private val profileService: IUserProjectProfileService,
	private val principalCache: IPrincipalCacheService,
	private val transactionalOperator: TransactionalOperator,
) : IProjectUseCase {
	override fun createProject(currentUser: CurrentUserModel, project: ProjectModel): Mono<ProjectModel> {
		return projectService.createProject(currentUser, project)
			.flatMap { createAdministratorProfile(currentUser, it) }
			.flatMap { projectService.findProjectWithProfileById(currentUser, it.id!!) }
			.`as`(transactionalOperator::transactional)
			.doOnNext { principalCache.invalidate(currentUser.oidcId) }
	}

	private fun createAdministratorProfile(currentUser: CurrentUserModel, project: ProjectModel): Mono<ProjectModel> {
		return profileService.createUserProjectProfileFromProject(currentUser, project)
			.thenReturn(project)
	}
}
