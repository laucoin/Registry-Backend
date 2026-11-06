package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.service.core.IUserProjectProfileService
import fr.laucoin.registry.backend.domain.service.usecase.IUserProjectProfileUseCase
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * Objectif : Point d'entrée des profils de projet de l'utilisateur courant pour la couche REST.
 * Périmètre : Délègue chaque opération au service UserProjectProfile sans logique propre.
 * Limites : Ne porte aucune règle métier ; elles restent dans le service.
 */
@Service
class UserProjectProfileUseCase(private val service: IUserProjectProfileService) : IUserProjectProfileUseCase {
	override fun findProjectProfilesPage(
		userId: UUID,
		pageable: PageableModel,
		searchParams: ProjectProfileSearchParamModel,
	): Mono<PageModel<ProjectProfileModel>> {
		return service.findProjectProfilesPage(userId, pageable, searchParams)
	}

	override fun findProjectProfilesById(currentUser: CurrentUserModel, id: UUID): Mono<ProjectProfileModel> {
		return service.findProjectProfilesById(currentUser, id)
	}

	override fun updateUserProjectProfileStatusById(
		currentUser: CurrentUserModel,
		id: UUID,
		status: ProfileStatusEnum,
	): Mono<ProjectProfileModel> {
		return service.updateUserProjectProfileStatusById(currentUser, id, status)
	}

	override fun createSupportProjectProfile(
		currentUser: CurrentUserModel,
		projectId: UUID,
	): Mono<ProjectProfileModel> {
		return service.createSupportProjectProfile(currentUser, projectId)
	}

	override fun deleteUserProjectProfileById(currentUser: CurrentUserModel, id: UUID): Mono<Unit> {
		return service.deleteUserProjectProfileById(currentUser, id)
	}
}
