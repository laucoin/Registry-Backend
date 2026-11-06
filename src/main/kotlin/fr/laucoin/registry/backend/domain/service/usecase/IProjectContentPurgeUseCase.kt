package fr.laucoin.registry.backend.domain.service.usecase

import fr.laucoin.registry.backend.domain.model.ProjectContentPurgeModel
import java.time.LocalDate
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat de la purge de rétention du contenu des projets (mouvements, alertes, communications).
 * Périmètre : Déclare le balayage du contenu, qui précède celui de la configuration.
 * Limites : Contrat uniquement.
 */
interface IProjectContentPurgeUseCase {
	fun purgeProjectsContents(dateThreshold: LocalDate?, dryRun: Boolean): Mono<ProjectContentPurgeModel>
}
