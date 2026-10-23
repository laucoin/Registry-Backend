package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.service.IActivityService
import fr.laucoin.registry.backend.domain.service.IAlertService
import fr.laucoin.registry.backend.domain.service.ICommunicationService
import fr.laucoin.registry.backend.domain.service.IGroupService
import fr.laucoin.registry.backend.domain.service.IMovementService
import fr.laucoin.registry.backend.domain.service.IParticipantService
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.domain.service.IUserService
import fr.laucoin.registry.backend.domain.service.IVehicleService
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

class PurgeServiceTest {
	private val userService: IUserService = mock()
	private val projectService: IProjectService = mock()
	private val movementService: IMovementService = mock()
	private val alertService: IAlertService = mock()
	private val communicationService: ICommunicationService = mock()
	private val activityService: IActivityService = mock()
	private val vehicleService: IVehicleService = mock()
	private val participantService: IParticipantService = mock()
	private val groupService: IGroupService = mock()
	private val transactionalOperator: TransactionalOperator = mock()
	private val clock: Clock = Clock.fixed(Instant.parse("2026-10-12T10:00:00Z"), UTC)
	private val service = PurgeService(
		userService, projectService, movementService, alertService, communicationService,
		activityService, vehicleService, participantService, groupService, clock,
		transactionalOperator,
		USERS_MONTHS, PROJECTS_MONTHS, CONTENT_MONTHS, CONFIGURATION_MONTHS,
	)

	private companion object {
		private const val USERS_MONTHS = 6L
		private const val PROJECTS_MONTHS = 12L
		private const val CONTENT_MONTHS = 3L
		private const val CONFIGURATION_MONTHS = 24L
	}

	@BeforeEach
	fun setup() {
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<Mono<*>>(0) }
	}

	@Test
	fun `Should purgeUsers use the configured threshold when no date is given`() {
		// Arrange
		val ids = listOf(UUID.randomUUID())
		whenever(userService.purgeUsersIfNecessary(LocalDate.of(2026, 4, 12), false)).thenReturn(Flux.fromIterable(ids))

		// Act
		val result = service.purgeUsers(dateThreshold = null, dryRun = false)

		// Assert
		StepVerifier.create(result).expectNext(ids[0]).verifyComplete()
	}

	@Test
	fun `Should purgeUsers prefer the explicit date over the configured threshold`() {
		// Arrange
		val threshold = LocalDate.of(2025, 1, 1)
		whenever(userService.purgeUsersIfNecessary(threshold, true)).thenReturn(Flux.empty())

		// Act
		val result = service.purgeUsers(threshold, dryRun = true)

		// Assert
		StepVerifier.create(result).verifyComplete()
		verify(userService).purgeUsersIfNecessary(threshold, true)
	}

	@Test
	fun `Should purgeProjects use the configured threshold when no date is given`() {
		// Arrange
		whenever(projectService.purgeProjectsIfNecessary(LocalDate.of(2025, 10, 12), true)).thenReturn(Flux.empty())

		// Act
		val result = service.purgeProjects(dateThreshold = null, dryRun = true)

		// Assert
		StepVerifier.create(result).verifyComplete()
		verify(projectService).purgeProjectsIfNecessary(LocalDate.of(2025, 10, 12), true)
	}

	@Test
	fun `Should purgeProjectsContents purge orphan communications of the purged movements and alerts`() {
		// Arrange
		val movementId = UUID.randomUUID()
		val alertId = UUID.randomUUID()
		val communicationId = UUID.randomUUID()
		val threshold = LocalDate.of(2026, 7, 12)
		whenever(movementService.purgeMovementsIfNecessary(threshold, false)).thenReturn(Flux.just(movementId))
		whenever(alertService.purgeAlertsIfNecessary(threshold, false)).thenReturn(Flux.just(alertId))
		whenever(communicationService.purgeOrphanCommunications(listOf(movementId), listOf(alertId), false))
			.thenReturn(Flux.just(communicationId))

		// Act
		val result = service.purgeProjectsContents(dateThreshold = null, dryRun = false)

		// Assert
		StepVerifier.create(result)
			.expectNextMatches {
				it.movements == listOf(movementId) && it.alerts == listOf(alertId) &&
					it.communications == listOf(communicationId)
			}
			.verifyComplete()
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
	fun `Should purgeProjectsContents prefer the explicit date over the configured threshold`() {
		// Arrange
		val threshold = LocalDate.of(2025, 1, 1)
		whenever(movementService.purgeMovementsIfNecessary(threshold, true)).thenReturn(Flux.empty())
		whenever(alertService.purgeAlertsIfNecessary(threshold, true)).thenReturn(Flux.empty())
		whenever(communicationService.purgeOrphanCommunications(emptyList(), emptyList(), true)).thenReturn(Flux.empty())

		// Act
		val result = service.purgeProjectsContents(threshold, dryRun = true)

		// Assert
		StepVerifier.create(result).expectNextCount(1).verifyComplete()
		verify(movementService).purgeMovementsIfNecessary(threshold, true)
		verify(alertService).purgeAlertsIfNecessary(threshold, true)
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
	fun `Should purgeProjectsContents run inside a transaction`() {
		// Arrange
		whenever(movementService.purgeMovementsIfNecessary(any(), any())).thenReturn(Flux.empty())
		whenever(alertService.purgeAlertsIfNecessary(any(), any())).thenReturn(Flux.empty())
		whenever(communicationService.purgeOrphanCommunications(any(), any(), any())).thenReturn(Flux.empty())

		// Act
		service.purgeProjectsContents(dateThreshold = null, dryRun = true).block()

		// Assert
		verify(transactionalOperator).transactional(any<Mono<*>>())
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
