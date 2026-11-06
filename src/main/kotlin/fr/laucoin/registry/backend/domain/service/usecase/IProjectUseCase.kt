package fr.laucoin.registry.backend.domain.service.usecase

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
}
