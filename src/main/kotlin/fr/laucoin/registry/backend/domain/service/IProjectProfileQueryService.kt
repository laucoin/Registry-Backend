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
 * Objectif : Contrat de lecture des profils de projet.
 * Périmètre : Déclare la consultation (page, détail), le sélecteur d'utilisateurs et les rôles attribuables.
 * Limites : Contrat uniquement ; ne modifie aucun profil.
 */
interface IProjectProfileQueryService {
	fun findProjectProfilesPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ProjectProfileSearchParamModel,
	): Mono<PageModel<ProjectProfileModel>>

	fun findProjectProfileById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<ProjectProfileModel>

	fun searchUsers(query: String?): Flux<UserModel>

	fun getAssignableProjectRoles(currentUser: CurrentUserModel, projectId: UUID): Flux<String>
}
