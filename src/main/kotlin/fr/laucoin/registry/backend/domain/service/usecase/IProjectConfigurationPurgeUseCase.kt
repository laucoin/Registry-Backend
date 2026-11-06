package fr.laucoin.registry.backend.domain.service.usecase

import fr.laucoin.registry.backend.domain.model.ProjectConfigurationPurgeModel
import java.time.LocalDate
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat de la purge de rétention de la configuration des projets (véhicules, participants, activités, groupes).
 * Périmètre : Déclare le balayage de configuration, exécuté après celui du contenu.
 * Limites : Contrat uniquement.
 */
interface IProjectConfigurationPurgeUseCase {
	fun purgeProjectsConfigurations(dateThreshold: LocalDate?, dryRun: Boolean): Mono<ProjectConfigurationPurgeModel>
}
