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

class PlatformPurgeUseCaseTest {
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
	private val service = PlatformPurgeUseCase(
		userService, projectService, auditService, PurgeThresholdHelper(clock),
		PurgeFeatureModel(
		PurgeFeatureModel.Threshold(USERS_MONTHS),
		PurgeFeatureModel.Projects(
			PROJECTS_MONTHS,
			PurgeFeatureModel.Threshold(CONTENT_MONTHS),
			PurgeFeatureModel.Threshold(CONFIGURATION_MONTHS),
		),
		PurgeFeatureModel.Threshold(AUDIT_MONTHS),
	),
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
	fun `Should purgeAudits use the configured threshold when no date is given`() {
		// Arrange
		whenever(auditService.purgeAuditsIfNecessary(LocalDate.of(2025, 4, 12), true)).thenReturn(Mono.just(7L))

		// Act
		val result = service.purgeAudits(dateThreshold = null, dryRun = true)

		// Assert
		StepVerifier.create(result).expectNext(7L).verifyComplete()
	}

	@Test
	fun `Should purgeAudits prefer the explicit date over the configured threshold`() {
		// Arrange
		val threshold = LocalDate.of(2025, 1, 1)
		whenever(auditService.purgeAuditsIfNecessary(threshold, false)).thenReturn(Mono.just(0L))

		// Act
		val result = service.purgeAudits(threshold, dryRun = false)

		// Assert
		StepVerifier.create(result).expectNext(0L).verifyComplete()
		verify(auditService).purgeAuditsIfNecessary(threshold, false)
	}
}
