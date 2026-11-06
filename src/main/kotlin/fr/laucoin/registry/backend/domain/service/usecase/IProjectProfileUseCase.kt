package fr.laucoin.registry.backend.domain.service.usecase

import reactor.core.publisher.Flux
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import java.util.UUID
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat de cas d'usage de l'invitation d'utilisateurs à un projet.
 * Périmètre : Déclare la création de profils de projet à partir d'adresses e-mail, y compris pour les inconnus.
 * Limites : Contrat uniquement ; les autres opérations sur les profils restent dans le service des profils.
 */
interface IProjectProfileUseCase {
	fun createProjectProfiles(
		currentUser: CurrentUserModel,
		projectId: UUID,
		profiles: List<ProjectProfileModel>,
	): Mono<Pair<List<String>, List<String>>>

	fun findProjectProfilesPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ProjectProfileSearchParamModel,
	): Mono<PageModel<ProjectProfileModel>>
	fun findProjectProfileById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<ProjectProfileModel>
	fun searchUsers(query: String?): Flux<UserModel>
	fun getAssignableProjectRoles(currentUser: CurrentUserModel, projectId: UUID): Flux<String>
	fun updateProjectProfileById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		profile: ProjectProfileModel,
	): Mono<ProjectProfileModel>
	fun blockProjectProfileById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<ProjectProfileModel>
	fun unblockProjectProfileById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<ProjectProfileModel>
	fun deleteProjectProfileById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Unit>
}
