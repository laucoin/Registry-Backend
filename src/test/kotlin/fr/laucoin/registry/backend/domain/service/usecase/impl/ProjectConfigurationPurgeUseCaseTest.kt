package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.service.core.IActivityService
import fr.laucoin.registry.backend.domain.service.core.IAlertService
import fr.laucoin.registry.backend.domain.service.core.ICommunicationService
import fr.laucoin.registry.backend.domain.service.core.IGroupService
import fr.laucoin.registry.backend.domain.service.core.IMovementService
import fr.laucoin.registry.backend.domain.service.core.IParticipantService
import fr.laucoin.registry.backend.domain.service.core.IProjectService
import fr.laucoin.registry.backend.domain.service.core.IUserService
import fr.laucoin.registry.backend.domain.service.core.IVehicleService
import fr.laucoin.registry.backend.domain.service.support.IAuditService
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.util.UUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

import fr.laucoin.registry.backend.domain.helper.PurgeThresholdHelper
import fr.laucoin.registry.backend.domain.model.PurgeFeatureModel

class ProjectConfigurationPurgeUseCaseTest {
	private val userService: IUserService = mock()
	private val projectService: IProjectService = mock()
	private val movementService: IMovementService = mock()
	private val alertService: IAlertService = mock()
	private val communicationService: ICommunicationService = mock()
	private val activityService: IActivityService = mock()
	private val vehicleService: IVehicleService = mock()
	private val participantService: IParticipantService = mock()
	private val groupService: IGroupService = mock()
	private val auditService: IAuditService = mock()
	private val transactionalOperator: TransactionalOperator = mock()
	private val clock: Clock = Clock.fixed(Instant.parse("2026-10-12T10:00:00Z"), UTC)
	private val service = ProjectConfigurationPurgeUseCase(
		vehicleService, participantService, activityService, groupService, PurgeThresholdHelper(clock),
		PurgeFeatureModel(
		PurgeFeatureModel.Threshold(USERS_MONTHS),
		PurgeFeatureModel.Projects(
			PROJECTS_MONTHS,
			PurgeFeatureModel.Threshold(CONTENT_MONTHS),
			PurgeFeatureModel.Threshold(CONFIGURATION_MONTHS),
		),
		PurgeFeatureModel.Threshold(AUDIT_MONTHS),
	),
		transactionalOperator,
	)

	private companion object {
		private const val USERS_MONTHS = 6L
		private const val PROJECTS_MONTHS = 12L
		private const val CONTENT_MONTHS = 3L
		private const val CONFIGURATION_MONTHS = 24L
		private const val AUDIT_MONTHS = 18L
	}

	@BeforeEach
	fun setup() {
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<Mono<*>>(0) }
	}

	@Test
	fun `Should purgeProjectsConfigurations purge empty groups of the purged participants`() {
		// Arrange
		val vehicleId = UUID.randomUUID()
		val participantId = UUID.randomUUID()
		val activityId = UUID.randomUUID()
		val groupId = UUID.randomUUID()
		val threshold = LocalDate.of(2024, 10, 12)
		whenever(vehicleService.purgeVehiclesIfNecessary(threshold, true)).thenReturn(Flux.just(vehicleId))
		whenever(participantService.purgeParticipantsIfNecessary(threshold, true)).thenReturn(Flux.just(participantId))
		whenever(activityService.purgeActivitiesIfNecessary(threshold, true)).thenReturn(Flux.just(activityId))
		whenever(groupService.purgeEmptyGroups(listOf(participantId), true)).thenReturn(Flux.just(groupId))

		// Act
		val result = service.purgeProjectsConfigurations(dateThreshold = null, dryRun = true)

		// Assert
		StepVerifier.create(result)
			.expectNextMatches {
				it.vehicles == listOf(vehicleId) && it.participants == listOf(participantId) &&
					it.activities == listOf(activityId) && it.groups == listOf(groupId)
			}
			.verifyComplete()
	}

	@Test
	fun `Should purgeProjectsConfigurations prefer the explicit date over the configured threshold`() {
		// Arrange
		val threshold = LocalDate.of(2025, 1, 1)
		whenever(vehicleService.purgeVehiclesIfNecessary(threshold, true)).thenReturn(Flux.empty())
		whenever(participantService.purgeParticipantsIfNecessary(threshold, true)).thenReturn(Flux.empty())
		whenever(activityService.purgeActivitiesIfNecessary(threshold, true)).thenReturn(Flux.empty())
		whenever(groupService.purgeEmptyGroups(emptyList(), true)).thenReturn(Flux.empty())

		// Act
		val result = service.purgeProjectsConfigurations(threshold, dryRun = true)

		// Assert
		StepVerifier.create(result).expectNextCount(1).verifyComplete()
		verify(vehicleService).purgeVehiclesIfNecessary(threshold, true)
		verify(participantService).purgeParticipantsIfNecessary(threshold, true)
		verify(activityService).purgeActivitiesIfNecessary(threshold, true)
	}

	@Test
	fun `Should purgeProjectsConfigurations run inside a transaction`() {
		// Arrange
		whenever(vehicleService.purgeVehiclesIfNecessary(any(), any())).thenReturn(Flux.empty())
		whenever(participantService.purgeParticipantsIfNecessary(any(), any())).thenReturn(Flux.empty())
		whenever(activityService.purgeActivitiesIfNecessary(any(), any())).thenReturn(Flux.empty())
		whenever(groupService.purgeEmptyGroups(any(), any())).thenReturn(Flux.empty())

		// Act
		service.purgeProjectsConfigurations(dateThreshold = null, dryRun = true).block()

		// Assert
		verify(transactionalOperator).transactional(any<Mono<*>>())
	}
}
