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
import fr.laucoin.registry.backend.infrastructure.out.api.controller.IPurgeV2Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectConfigurationPurgeReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectContentPurgeReaderDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.NOT_IMPLEMENTED
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Objects
import java.util.UUID

/**
 * API v2 Purge endpoints. The cron scheduling stays on [PurgeV1Controller]
 * only, until v1 is sunset — duplicating the `@Scheduled` triggers here would
 * run every purge twice.
 */
@RestController
class PurgeV2Controller(
	private val userService: IUserService,
	@param:Value($$"${registry.feature.purge.users.months-threshold:}")
	private val userPurgeMonthThreshold: Long?,
	@param:Value($$"${registry.feature.purge.light-users.months-threshold:}")
	private val lightUserPurgeMonthThreshold: Long?,
	private val projectService: IProjectService,
	@param:Value($$"${registry.feature.purge.projects.months-threshold:}")
	private val projectPurgeMonthThreshold: Long?,
	private val movementService: IMovementService,
	private val alertService: IAlertService,
	private val communicationService: ICommunicationService,
	@param:Value($$"${registry.feature.purge.projects.content.months-threshold:}")
	private val projectContentPurgeMonthThreshold: Long?,
	private val activityService: IActivityService,
	private val vehicleService: IVehicleService,
	private val participantService: IParticipantService,
	private val groupService: IGroupService,
	@param:Value($$"${registry.feature.purge.projects.configuration.months-threshold:}")
	private val projectConfigurationPurgeMonthThreshold: Long?,
) : IPurgeV2Controller {
	override fun purgeUsersIfNecessary(dateThreshold: LocalDate?, dryRun: Boolean): Flux<UUID> {
		val deleteOlderThan = buildDateThreshold(userPurgeMonthThreshold, dateThreshold)
		return userService.purgeUsersIfNecessary(deleteOlderThan, dryRun)
	}

	override fun purgeLightUsersIfNecessary(dateThreshold: LocalDate?, dryRun: Boolean): Flux<UUID> {
		val deleteOlderThan = buildDateThreshold(lightUserPurgeMonthThreshold, dateThreshold)
		return userService.purgeLightUsersIfNecessary(deleteOlderThan, dryRun)
	}

	override fun purgeProjectsIfNecessary(dateThreshold: LocalDate?, dryRun: Boolean): Flux<UUID> {
		val deleteOlderThan = buildDateThreshold(projectPurgeMonthThreshold, dateThreshold)
		return projectService.purgeProjectsIfNecessary(deleteOlderThan, dryRun)
	}

	override fun purgeProjectsContentsIfNecessary(
		dateThreshold: LocalDate?,
		dryRun: Boolean,
	): Mono<ProjectContentPurgeReaderDto> {
		val deleteOlderThan = buildDateThreshold(projectContentPurgeMonthThreshold, dateThreshold)
		val purgeMovement = movementService.purgeMovementsIfNecessary(deleteOlderThan, dryRun).collectList()
		val purgeAlert = alertService.purgeAlertsIfNecessary(deleteOlderThan, dryRun).collectList()

		return Mono.zip(purgeMovement, purgeAlert).flatMap { mvAltIds ->
			communicationService.purgeOrphanCommunications(mvAltIds.t1, mvAltIds.t2, dryRun)
				.collectList()
				.map { ProjectContentPurgeReaderDto(mvAltIds.t1, mvAltIds.t2, it) }
		}
	}

	override fun purgeProjectsConfigurationsIfNecessary(
		dateThreshold: LocalDate?,
		dryRun: Boolean,
	): Mono<ProjectConfigurationPurgeReaderDto> {
		val deleteOlderThan = buildDateThreshold(projectConfigurationPurgeMonthThreshold, dateThreshold)
		val purgeVehicle = vehicleService.purgeVehiclesIfNecessary(deleteOlderThan, dryRun).collectList()
		val purgeParticipant = participantService.purgeParticipantsIfNecessary(deleteOlderThan, dryRun).collectList()
		val purgeActivity = activityService.purgeActivitiesIfNecessary(deleteOlderThan, dryRun).collectList()

		return Mono.zip(purgeVehicle, purgeParticipant, purgeActivity).flatMap { vclPptAvtIds ->
			groupService.purgeEmptyGroups(vclPptAvtIds.t2, dryRun).collectList()
				.map {
					ProjectConfigurationPurgeReaderDto(vclPptAvtIds.t1, vclPptAvtIds.t2, vclPptAvtIds.t3, it)
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
