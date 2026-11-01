package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.ProjectConfigurationPurgeModel
import fr.laucoin.registry.backend.domain.model.ProjectContentPurgeModel
import java.time.LocalDate
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Exécute les balayages de rétention qui suppriment définitivement les utilisateurs, projets, contenus et configurations inactifs.
 * Périmètre : Résout le seuil de date (explicite ou configuré) et orchestre les purges par ressource.
 * Limites : Ne se planifie pas lui-même et n'autorise pas les appelants.
 */
interface IPurgeService {
	fun purgeUsers(dateThreshold: LocalDate?, dryRun: Boolean): Flux<UUID>
	fun purgeProjects(dateThreshold: LocalDate?, dryRun: Boolean): Flux<UUID>
	fun purgeProjectsContents(dateThreshold: LocalDate?, dryRun: Boolean): Mono<ProjectContentPurgeModel>
	fun purgeProjectsConfigurations(dateThreshold: LocalDate?, dryRun: Boolean): Mono<ProjectConfigurationPurgeModel>
}
