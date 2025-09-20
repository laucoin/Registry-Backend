package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_IMPLEMENTED_YET
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.service.IActivityService
import fr.laucoin.registry.backend.domain.service.IAlertService
import fr.laucoin.registry.backend.domain.service.ICommunicationService
import fr.laucoin.registry.backend.domain.service.IGroupService
import fr.laucoin.registry.backend.domain.service.IMovementService
import fr.laucoin.registry.backend.domain.service.IParticipantService
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.domain.service.IUserService
import fr.laucoin.registry.backend.domain.service.IVehicleService
import fr.laucoin.registry.backend.infrastructure.out.api.controller.IPurgeController
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectConfigurationPurgeReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectContentPurgeReaderDto
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Objects
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.NOT_IMPLEMENTED
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class PurgeController(
	private val userService: IUserService,
	@param:Value("\${registry.feature.purge.users.months-threshold:}")
	private val userPurgeMonthThreshold: Long?,
	private val projectService: IProjectService,
	@param:Value("\${registry.feature.purge.projects.months-threshold:}")
	private val projectPurgeMonthThreshold: Long?,
	private val movementService: IMovementService,
	private val alertService: IAlertService,
	private val communicationService: ICommunicationService,
	@param:Value("\${registry.feature.purge.projects.content.months-threshold:}")
	private val projectContentPurgeMonthThreshold: Long?,
	private val activityService: IActivityService,
	private val vehicleService: IVehicleService,
	private val participantService: IParticipantService,
	private val groupService: IGroupService,
	@param:Value("\${registry.feature.purge.projects.configuration.months-threshold:}")
	private val projectConfigurationPurgeMonthThreshold: Long?,
): IPurgeController {
	override fun purgeUsersIfNecessary(dateThreshold: LocalDate?, dryRun: Boolean): Flux<UUID> {
		return launchPurgeUsers(dateThreshold, dryRun)
	}

	@Scheduled(cron = "\${registry.feature.purge.users.cron}")
	fun scheduledUsersPurge() {
		launchPurgeUsers(dateThreshold = null, dryRun = false).subscribe()
	}

	private fun launchPurgeUsers(dateThreshold: LocalDate?, dryRun: Boolean): Flux<UUID> {
		val deleteOlderThan = buildDateThreshold(userPurgeMonthThreshold, dateThreshold)
		return userService.purgeUsersIfNecessary(deleteOlderThan, dryRun)
	}

	override fun purgeProjectsIfNecessary(dateThreshold: LocalDate?, dryRun: Boolean): Flux<UUID> {
		return launchPurgeProjects(dateThreshold, dryRun)
	}

	@Scheduled(cron = "\${registry.feature.purge.projects.cron}")
	fun scheduledProjectsPurge() {
		launchPurgeProjects(dateThreshold = null, dryRun = false).subscribe()
	}

	private fun launchPurgeProjects(dateThreshold: LocalDate?, dryRun: Boolean): Flux<UUID> {
		val deleteOlderThan = buildDateThreshold(projectPurgeMonthThreshold, dateThreshold)
		return projectService.purgeProjectsIfNecessary(deleteOlderThan, dryRun)
	}

	override fun purgeProjectsContentsIfNecessary(
		dateThreshold: LocalDate?,
		dryRun: Boolean
	): Mono<ProjectContentPurgeReaderDto> {
		return launchPurgeProjectsContents(dateThreshold, dryRun)
	}

	@Scheduled(cron = "\${registry.feature.purge.projects.content.cron}")
	fun scheduledProjectsContentsPurge() {
		launchPurgeProjectsContents(dateThreshold = null, dryRun = false).subscribe()
	}

	private fun launchPurgeProjectsContents(
		dateThreshold: LocalDate?,
		dryRun: Boolean
	): Mono<ProjectContentPurgeReaderDto> {
		val deleteOlderThan = buildDateThreshold(projectContentPurgeMonthThreshold, dateThreshold)
		return Mono.zip(
			movementService.purgeMovementsIfNecessary(deleteOlderThan, dryRun).collectList(),
			alertService.purgeAlertsIfNecessary(deleteOlderThan, dryRun).collectList(),
		).flatMap { movementAndAlertIds ->
			communicationService.purgeOrphanCommunications(movementAndAlertIds.t1, movementAndAlertIds.t2, dryRun)
				.collectList()
				.map {
					ProjectContentPurgeReaderDto(
						movementAndAlertIds.t1,
						movementAndAlertIds.t2,
						it,
					)
				}
		}
	}

	override fun purgeProjectsConfigurationsIfNecessary(
		dateThreshold: LocalDate?,
		dryRun: Boolean
	): Mono<ProjectConfigurationPurgeReaderDto> {
		return launchPurgeProjectsConfigurations(dateThreshold, dryRun)
	}

	@Scheduled(cron = "\${registry.feature.purge.projects.configuration.cron}")
	fun scheduledProjectsConfigurationsPurge() {
		launchPurgeProjectsConfigurations(dateThreshold = null, dryRun = false).subscribe()
	}

	private fun launchPurgeProjectsConfigurations(
		dateThreshold: LocalDate?,
		dryRun: Boolean
	): Mono<ProjectConfigurationPurgeReaderDto> {
		val deleteOlderThan = buildDateThreshold(projectConfigurationPurgeMonthThreshold, dateThreshold)
		return Mono.zip(
			vehicleService.purgeVehiclesIfNecessary(deleteOlderThan, dryRun).collectList(),
			participantService.purgeParticipantsIfNecessary(deleteOlderThan, dryRun).collectList(),
			activityService.purgeActivitiesIfNecessary(deleteOlderThan, dryRun).collectList(),
		).flatMap { vehicleParticipantAndActivityIds ->
			groupService.purgeEmptyGroups(vehicleParticipantAndActivityIds.t2, dryRun).collectList()
				.map {
					ProjectConfigurationPurgeReaderDto(
						vehicleParticipantAndActivityIds.t1,
						vehicleParticipantAndActivityIds.t2,
						vehicleParticipantAndActivityIds.t3,
						it
					)
				}
		}
	}

	private fun buildDateThreshold(thresholdInMonth: Long?, dateThreshold: LocalDate?): LocalDate {
		if (Objects.isNull(thresholdInMonth) && Objects.isNull(dateThreshold)) {
			throw RegistryException(NOT_IMPLEMENTED, NOT_IMPLEMENTED_YET)
		}

		return dateThreshold ?: LocalDate.ofInstant(
			Instant.now(),
			ZoneId.of("UTC")
		).minusMonths(thresholdInMonth!!)
	}
}
