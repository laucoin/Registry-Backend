package fr.laucoin.registry.backend.domain.service.core

import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import java.util.UUID
import reactor.core.publisher.Flux

/**
 * Objectif : Contrat de recherche des mouvements et des alertes à lier à une communication.
 * Périmètre : Déclare la recherche textuelle bornée des mouvements de sortie avec activité et des alertes en cours.
 * Limites : Contrat uniquement ; les limites de résultats viennent de la configuration.
 */
interface ICommunicationSearchService {
	fun searchOutMovementWithActivityByText(projectId: UUID, query: String?): Flux<MovementModel>
	fun searchAlertByText(projectId: UUID, query: String?): Flux<AlertModel>
}
