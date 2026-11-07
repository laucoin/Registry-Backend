package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.helper.PurgeThresholdHelper
import fr.laucoin.registry.backend.domain.model.ProjectConfigurationPurgeModel
import fr.laucoin.registry.backend.domain.model.PurgeFeatureModel
import fr.laucoin.registry.backend.domain.service.core.IActivityService
import fr.laucoin.registry.backend.domain.service.core.IGroupService
import fr.laucoin.registry.backend.domain.service.core.IParticipantService
import fr.laucoin.registry.backend.domain.service.core.IVehicleService
import fr.laucoin.registry.backend.domain.service.usecase.IProjectConfigurationPurgeUseCase
import java.time.LocalDate
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono

/**
 * Objectif : Purge la configuration des projets (véhicules, participants, activités, groupes vides) au-delà de sa durée de rétention.
 * Périmètre : Supprime véhicules, participants et activités anciens puis les groupes devenus vides, dans une même transaction.
 * Limites : Ne purge pas le contenu des projets ; il a son propre balayage, exécuté avant.
 */
@Service
class ProjectConfigurationPurgeUseCase(
	private val vehicleService: IVehicleService,
	private val participantService: IParticipantService,
	private val activityService: IActivityService,
	private val groupService: IGroupService,
	private val threshold: PurgeThresholdHelper,
	private val purge: PurgeFeatureModel,
	private val transactionalOperator: TransactionalOperator,
) : IProjectConfigurationPurgeUseCase {
	override fun purgeProjectsConfigurations(
		dateThreshold: LocalDate?,
		dryRun: Boolean,
	): Mono<ProjectConfigurationPurgeModel> {
		val deleteOlderThan = threshold.resolve(purge.projects.configuration.monthsThreshold, dateThreshold)
		val purgeVehicles = vehicleService.purgeVehiclesIfNecessary(deleteOlderThan, dryRun).collectList()
		val purgeParticipants = participantService.purgeParticipantsIfNecessary(deleteOlderThan, dryRun).collectList()
		val purgeActivities = activityService.purgeActivitiesIfNecessary(deleteOlderThan, dryRun).collectList()

		return Mono.zip(purgeVehicles, purgeParticipants, purgeActivities)
			.flatMap { purgeEmptyGroups(it.t1, it.t2, it.t3, dryRun) }
			.`as`(transactionalOperator::transactional)
	}

	private fun purgeEmptyGroups(
		vehicles: List<UUID>,
		participants: List<UUID>,
		activities: List<UUID>,
		dryRun: Boolean,
	): Mono<ProjectConfigurationPurgeModel> {
		return groupService.purgeEmptyGroups(participants, dryRun)
			.collectList()
			.map { ProjectConfigurationPurgeModel(vehicles, participants, activities, it) }
	}
}
