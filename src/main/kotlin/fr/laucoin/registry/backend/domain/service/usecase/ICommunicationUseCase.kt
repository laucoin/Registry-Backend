package fr.laucoin.registry.backend.domain.service.usecase

import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat de cas d'usage des communications exposé à la couche REST.
 * Périmètre : Reprend les opérations du service Communication appelées par les contrôleurs.
 * Limites : Contrat uniquement ; les règles métier vivent dans le service.
 */
interface ICommunicationUseCase {
	fun findCommunicationById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<CommunicationModel>
	fun searchOutMovementWithActivityByText(projectId: UUID, query: String?): Flux<MovementModel>
	fun searchAlertByText(projectId: UUID, query: String?): Flux<AlertModel>
	fun createCommunication(
		currentUser: CurrentUserModel,
		communication: CommunicationModel,
	): Mono<CommunicationModel>
	fun updateCommunicationById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		communication: CommunicationModel,
	): Mono<CommunicationModel>
	fun disableCommunicationById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<CommunicationModel>
	fun enableCommunicationById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<CommunicationModel>
	fun deleteCommunicationById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Unit>
}
