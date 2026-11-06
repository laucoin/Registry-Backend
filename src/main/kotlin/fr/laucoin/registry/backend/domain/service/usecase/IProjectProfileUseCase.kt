package fr.laucoin.registry.backend.domain.service.usecase

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
}
