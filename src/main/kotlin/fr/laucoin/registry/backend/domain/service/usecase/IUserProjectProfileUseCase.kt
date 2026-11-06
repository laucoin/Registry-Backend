package fr.laucoin.registry.backend.domain.service.usecase

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import java.util.UUID
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat de cas d'usage des profils de projet de l'utilisateur courant exposé à la couche REST.
 * Périmètre : Reprend les opérations du service UserProjectProfile appelées par les contrôleurs.
 * Limites : Contrat uniquement ; les règles métier vivent dans le service.
 */
interface IUserProjectProfileUseCase {
	fun findProjectProfilesPage(
		userId: UUID,
		pageable: PageableModel,
		searchParams: ProjectProfileSearchParamModel,
	): Mono<PageModel<ProjectProfileModel>>
	fun findProjectProfilesById(currentUser: CurrentUserModel, id: UUID): Mono<ProjectProfileModel>
	fun updateUserProjectProfileStatusById(
		currentUser: CurrentUserModel,
		id: UUID,
		status: ProfileStatusEnum,
	): Mono<ProjectProfileModel>
	fun createSupportProjectProfile(currentUser: CurrentUserModel, projectId: UUID): Mono<ProjectProfileModel>
	fun deleteUserProjectProfileById(currentUser: CurrentUserModel, id: UUID): Mono<Unit>
}
