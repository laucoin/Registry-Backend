package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.helper.PurgeThresholdHelper
import fr.laucoin.registry.backend.domain.model.ProjectContentPurgeModel
import fr.laucoin.registry.backend.domain.model.PurgeFeatureModel
import fr.laucoin.registry.backend.domain.service.core.IAlertService
import fr.laucoin.registry.backend.domain.service.core.ICommunicationService
import fr.laucoin.registry.backend.domain.service.core.IMovementService
import fr.laucoin.registry.backend.domain.service.usecase.IProjectContentPurgeUseCase
import java.time.LocalDate
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono

/**
 * Objectif : Purge le contenu des projets (mouvements, alertes, communications orphelines) au-delà de sa durée de rétention.
 * Périmètre : Supprime les mouvements et alertes anciens puis les communications devenues orphelines, dans une même transaction.
 * Limites : Ne purge pas la configuration des projets ; elle a son propre balayage, exécuté après.
 */
@Service
class ProjectContentPurgeUseCase(
	private val movementService: IMovementService,
	private val alertService: IAlertService,
	private val communicationService: ICommunicationService,
	private val threshold: PurgeThresholdHelper,
	private val purge: PurgeFeatureModel,
	private val transactionalOperator: TransactionalOperator,
) : IProjectContentPurgeUseCase {
	override fun purgeProjectsContents(dateThreshold: LocalDate?, dryRun: Boolean): Mono<ProjectContentPurgeModel> {
		val deleteOlderThan = threshold.resolve(purge.projects.content.monthsThreshold, dateThreshold)
		val purgeMovements = movementService.purgeMovementsIfNecessary(deleteOlderThan, dryRun).collectList()
		val purgeAlerts = alertService.purgeAlertsIfNecessary(deleteOlderThan, dryRun).collectList()

		return Mono.zip(purgeMovements, purgeAlerts)
			.flatMap { purgeOrphanCommunications(it.t1, it.t2, dryRun) }
			.`as`(transactionalOperator::transactional)
	}

	private fun purgeOrphanCommunications(
		movements: List<UUID>,
		alerts: List<UUID>,
		dryRun: Boolean,
	): Mono<ProjectContentPurgeModel> {
		return communicationService.purgeOrphanCommunications(movements, alerts, dryRun)
			.collectList()
			.map { ProjectContentPurgeModel(movements, alerts, it) }
	}
}
