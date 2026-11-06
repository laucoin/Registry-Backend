package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.service.core.ICommunicationService
import fr.laucoin.registry.backend.domain.service.usecase.ICommunicationUseCase
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Point d'entrée des communications pour la couche REST.
 * Périmètre : Délègue chaque opération au service Communication sans logique propre.
 * Limites : Ne porte aucune règle métier ; elles restent dans le service.
 */
@Service
class CommunicationUseCase(private val service: ICommunicationService) : ICommunicationUseCase {
	override fun findCommunicationById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<CommunicationModel> {
		return service.findCommunicationById(projectId, id, isVisible)
	}

	override fun searchOutMovementWithActivityByText(projectId: UUID, query: String?): Flux<MovementModel> {
		return service.searchOutMovementWithActivityByText(projectId, query)
	}

	override fun searchAlertByText(projectId: UUID, query: String?): Flux<AlertModel> {
		return service.searchAlertByText(projectId, query)
	}

	override fun createCommunication(
		currentUser: CurrentUserModel,
		communication: CommunicationModel,
	): Mono<CommunicationModel> {
		return service.createCommunication(currentUser, communication)
	}

	override fun updateCommunicationById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		communication: CommunicationModel,
	): Mono<CommunicationModel> {
		return service.updateCommunicationById(currentUser, projectId, id, communication)
	}

	override fun disableCommunicationById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<CommunicationModel> {
		return service.disableCommunicationById(currentUser, projectId, id)
	}

	override fun enableCommunicationById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<CommunicationModel> {
		return service.enableCommunicationById(currentUser, projectId, id)
	}

	override fun deleteCommunicationById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Unit> {
		return service.deleteCommunicationById(currentUser, projectId, id)
	}
}
