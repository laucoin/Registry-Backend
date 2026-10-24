package fr.laucoin.registry.backend.infrastructure.driving.scheduler

import fr.laucoin.registry.backend.domain.service.IPurgeService
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * Objectif : Déclenche les quatre balayages de rétention selon leurs expressions cron configurées.
 * Périmètre : Porte la planification et la journalisation des exécutions en échec ; chaque exécution est une vraie purge avec le seuil configuré.
 * Limites : Ne porte aucune logique de purge ; une exécution en échec est journalisée et jamais rejouée avant le prochain déclenchement cron.
 */
@Component
class PurgeScheduler(
	private val purgeService: IPurgeService,
) {
	@Scheduled(cron = $$"${registry.feature.purge.users.cron}")
	fun purgeUsers() = launch("users", purgeService.purgeUsers(dateThreshold = null, dryRun = false))

	@Scheduled(cron = $$"${registry.feature.purge.projects.cron}")
	fun purgeProjects() = launch("projects", purgeService.purgeProjects(dateThreshold = null, dryRun = false))

	@Scheduled(cron = $$"${registry.feature.purge.projects.content.cron}")
	fun purgeProjectsContents() =
		launch("projects contents", purgeService.purgeProjectsContents(dateThreshold = null, dryRun = false))

	@Scheduled(cron = $$"${registry.feature.purge.projects.configuration.cron}")
	fun purgeProjectsConfigurations() =
		launch("projects configurations", purgeService.purgeProjectsConfigurations(dateThreshold = null, dryRun = false))

	private fun launch(job: String, purge: Publisher<*>) {
		Mono.from(purge)
			.doOnError { log.error("Scheduled purge \"{}\" failed", job, it) }
			.onErrorResume { Mono.empty() }
			.subscribe()
	}

	private companion object {
		val log = LoggerFactory.getLogger(PurgeScheduler::class.java)
	}
}
