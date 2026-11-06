package fr.laucoin.registry.backend.domain.service.usecase

import reactor.core.publisher.Flux
import java.util.UUID
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat de cas d'usage de la création d'un projet.
 * Périmètre : Déclare la création du projet avec le profil d'administrateur de son créateur.
 * Limites : Contrat uniquement ; les opérations propres au projet restent dans le service du projet.
 */
interface IProjectUseCase {
	fun createProject(currentUser: CurrentUserModel, project: ProjectModel): Mono<ProjectModel>

	fun findProjectsPage(
		currentUser: CurrentUserModel,
		pageable: PageableModel,
		withProfile: Boolean?,
		searchParams: ProjectSearchParamModel,
	): Mono<PageModel<ProjectModel>>
	fun findProjectById(id: UUID, isVisible: Boolean?): Mono<ProjectModel>
	fun findProjectWithProfileById(currentUser: CurrentUserModel, id: UUID): Mono<ProjectModel>
	fun availableProjectOptions(): Flux<ProjectOptionEnum>
	fun updateProjectById(currentUser: CurrentUserModel, id: UUID, project: ProjectModel): Mono<ProjectModel>
	fun disableProjectById(currentUser: CurrentUserModel, id: UUID): Mono<ProjectModel>
	fun enableProjectById(currentUser: CurrentUserModel, id: UUID): Mono<ProjectModel>
	fun addFavoriteProject(currentUser: CurrentUserModel, id: UUID): Mono<Unit>
	fun removeFavoriteProject(currentUser: CurrentUserModel, id: UUID): Mono<Unit>
	fun deleteProjectById(id: UUID): Mono<Unit>
}
