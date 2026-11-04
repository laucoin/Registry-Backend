package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat de lecture des projets.
 * Périmètre : Déclare la consultation (page, détail, avec profil de l'appelant) et les options disponibles.
 * Limites : Contrat uniquement ; ne modifie aucun projet.
 */
interface IProjectQueryService {
	fun findProjectsPage(
		currentUser: CurrentUserModel,
		pageable: PageableModel,
		withProfile: Boolean?,
		searchParams: ProjectSearchParamModel,
	): Mono<PageModel<ProjectModel>>

	fun findProjectById(id: UUID, isVisible: Boolean?): Mono<ProjectModel>

	fun findProjectWithProfileById(currentUser: CurrentUserModel, id: UUID): Mono<ProjectModel>

	fun availableProjectOptions(): Flux<ProjectOptionEnum>

	fun withCurrentUserProfile(project: ProjectModel, currentUser: CurrentUserModel): Mono<ProjectModel>

	fun withFavorite(project: ProjectModel, currentUser: CurrentUserModel): Mono<ProjectModel>
}
