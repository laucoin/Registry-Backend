package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.model.ProjectConfigurationPurgeModel
import fr.laucoin.registry.backend.domain.model.ProjectContentPurgeModel
import fr.laucoin.registry.backend.domain.service.IActivityService
import fr.laucoin.registry.backend.domain.service.IAlertService
import fr.laucoin.registry.backend.domain.service.ICommunicationService
import fr.laucoin.registry.backend.domain.service.IGroupService
import fr.laucoin.registry.backend.domain.service.IMovementService
import fr.laucoin.registry.backend.domain.service.IParticipantService
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.domain.service.IPurgeService
import fr.laucoin.registry.backend.domain.service.IUserService
import fr.laucoin.registry.backend.domain.service.IVehicleService
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Purpose: Implements the retention sweeps for users, projects, project contents and project configurations.
 * Scope: Owns the threshold resolution and the delete ordering between dependent resources.
 * Limits: Delegates the actual deletion to the resource services; no scheduling, no authorization.
 */
@Service
class PurgeService(
	private val userService: IUserService,
	private val projectService: IProjectService,
	private val movementService: IMovementService,
	private val alertService: IAlertService,
	private val communicationService: ICommunicationService,
	private val activityService: IActivityService,
	private val vehicleService: IVehicleService,
	private val participantService: IParticipantService,
	private val groupService: IGroupService,
	private val clock: Clock,
	private val transactionalOperator: TransactionalOperator,
	@param:Value($$"${registry.feature.purge.users.months-threshold}")
	private val userMonthsThreshold: Long,
	@param:Value($$"${registry.feature.purge.projects.months-threshold}")
	private val projectMonthsThreshold: Long,
	@param:Value($$"${registry.feature.purge.projects.content.months-threshold}")
	private val contentMonthsThreshold: Long,
	@param:Value($$"${registry.feature.purge.projects.configuration.months-threshold}")
	private val configurationMonthsThreshold: Long,
) : IPurgeService {
	override fun purgeUsers(dateThreshold: LocalDate?, dryRun: Boolean): Flux<UUID> {
		return userService.purgeUsersIfNecessary(resolveThreshold(userMonthsThreshold, dateThreshold), dryRun)
	}

	override fun purgeProjects(dateThreshold: LocalDate?, dryRun: Boolean): Flux<UUID> {
		return projectService.purgeProjectsIfNecessary(resolveThreshold(projectMonthsThreshold, dateThreshold), dryRun)
	}

	override fun purgeProjectsContents(dateThreshold: LocalDate?, dryRun: Boolean): Mono<ProjectContentPurgeModel> {
		val deleteOlderThan = resolveThreshold(contentMonthsThreshold, dateThreshold)
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

	override fun purgeProjectsConfigurations(
		dateThreshold: LocalDate?,
		dryRun: Boolean,
	): Mono<ProjectConfigurationPurgeModel> {
		val deleteOlderThan = resolveThreshold(configurationMonthsThreshold, dateThreshold)
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

	private fun resolveThreshold(monthsThreshold: Long, dateThreshold: LocalDate?): LocalDate {
		return dateThreshold ?: LocalDate.now(clock).minusMonths(monthsThreshold)
	}
}
