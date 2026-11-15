package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.helper.PurgeThresholdHelper
import fr.laucoin.registry.backend.domain.model.PurgeFeatureModel
import fr.laucoin.registry.backend.domain.service.core.IProjectService
import fr.laucoin.registry.backend.domain.service.core.IUserService
import fr.laucoin.registry.backend.domain.service.support.IAuditService
import fr.laucoin.registry.backend.domain.service.usecase.IPlatformPurgeUseCase
import java.time.LocalDate
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Purge les utilisateurs, les projets et les événements d'audit au-delà de leur durée de rétention.
 * Périmètre : Résout le seuil (explicite ou configuré) puis délègue la suppression aux services de chaque ressource.
 * Limites : Ne planifie rien et n'autorise rien ; le contrôleur et le planificateur décident de l'appel.
 */
@Service
class PlatformPurgeUseCase(
	private val userService: IUserService,
	private val projectService: IProjectService,
	private val auditService: IAuditService,
	private val threshold: PurgeThresholdHelper,
	private val purge: PurgeFeatureModel,
) : IPlatformPurgeUseCase {
	override fun purgeUsers(dateThreshold: LocalDate?, dryRun: Boolean): Flux<UUID> {
		return userService.purgeUsersIfNecessary(threshold.resolve(purge.users.monthsThreshold, dateThreshold), dryRun)
	}

	override fun purgeProjects(dateThreshold: LocalDate?, dryRun: Boolean): Flux<UUID> {
		val deleteOlderThan = threshold.resolve(purge.projects.monthsThreshold, dateThreshold)
		return projectService.purgeProjectsIfNecessary(deleteOlderThan, dryRun)
	}

	override fun purgeAudits(dateThreshold: LocalDate?, dryRun: Boolean): Mono<Long> {
		return auditService.purgeAuditsIfNecessary(threshold.resolve(purge.audit.monthsThreshold, dateThreshold), dryRun)
	}
}
