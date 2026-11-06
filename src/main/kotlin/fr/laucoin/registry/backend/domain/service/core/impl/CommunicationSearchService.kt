package fr.laucoin.registry.backend.domain.service.core.impl

import fr.laucoin.registry.backend.domain.service.core.ICommunicationSearchService
import fr.laucoin.registry.backend.domain.model.CommunicationFeatureModel
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.port.IAlertPort
import fr.laucoin.registry.backend.domain.port.IMovementPort
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

/**
 * Objectif : Recherche les mouvements de sortie avec activité et les alertes proposés lors de la saisie d'une communication.
 * Périmètre : Applique les filtres de visibilité et la limite de résultats configurée pour chaque recherche.
 * Limites : Ne crée aucune communication ; ne renvoie que des éléments visibles et disponibles.
 */
@Service
class CommunicationSearchService(
	private val movementPort: IMovementPort,
	private val alertPort: IAlertPort,
	private val feature: CommunicationFeatureModel,
) : ICommunicationSearchService {
	override fun searchOutMovementWithActivityByText(
		projectId: UUID,
		query: String?
	): Flux<MovementModel> {
		return movementPort.findActivityWithLimit(
			feature.searched.maxActivityResult,
			projectId,
			ActivitySearchParamModel(
				query = query,
				isVisible = true,
				isAvailable = true,
				at = null,
			)
		)
	}

	override fun searchAlertByText(
		projectId: UUID,
		query: String?
	): Flux<AlertModel> {
		return alertPort.findWithLimit(
			feature.searched.maxAlertResult,
			projectId,
			AlertSearchParamModel(
				query,
				isVisible = true,
				status = AlertStatusEnum.IN_PROGRESS,
			)
		)
	}
}
