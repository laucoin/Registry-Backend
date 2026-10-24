package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.GenericModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * Objectif : Contrat de cas d'usage du profil de projet de l'utilisateur.
 * Périmètre : Déclare ce que les contrôleurs et les autres services peuvent demander à propos du profil de projet de l'utilisateur.
 * Limites : Contrat uniquement ; les règles métier vivent dans l'implémentation.
 */
interface IUserProjectProfileService {
	fun findProjectProfilesPage(
		userId: UUID,
		pageable: PageableModel,
		searchParams: ProjectProfileSearchParamModel,
	): Mono<PageModel<ProjectProfileModel>>

	fun findProjectProfilesById(currentUser: CurrentUserModel, id: UUID): Mono<ProjectProfileModel>


	fun <T : GenericModel> validateNotLastProjectRoleLevel0(
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
	): Mono<ProjectProfileModel>

	fun createSupportProjectProfile(currentUser: CurrentUserModel, projectId: UUID): Mono<ProjectProfileModel>
	fun deleteUserProjectProfileById(currentUser: CurrentUserModel, id: UUID): Mono<Unit>
}
