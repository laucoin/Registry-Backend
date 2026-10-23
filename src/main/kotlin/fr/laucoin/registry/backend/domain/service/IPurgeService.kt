package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.ProjectConfigurationPurgeModel
import fr.laucoin.registry.backend.domain.model.ProjectContentPurgeModel
import java.time.LocalDate
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Purpose: Runs the retention sweeps that permanently delete inactive users, projects, contents and configurations.
 * Scope: Resolves the date threshold (explicit or configured) and orchestrates the per-resource purges.
 * Limits: Does not schedule itself and does not authorize callers.
 */
interface IPurgeService {
	fun purgeUsers(dateThreshold: LocalDate?, dryRun: Boolean): Flux<UUID>
	fun purgeProjects(dateThreshold: LocalDate?, dryRun: Boolean): Flux<UUID>
	fun purgeProjectsContents(dateThreshold: LocalDate?, dryRun: Boolean): Mono<ProjectContentPurgeModel>
	fun purgeProjectsConfigurations(dateThreshold: LocalDate?, dryRun: Boolean): Mono<ProjectConfigurationPurgeModel>
}
