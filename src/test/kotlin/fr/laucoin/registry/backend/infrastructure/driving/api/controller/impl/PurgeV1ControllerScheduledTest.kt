package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

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
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux

class PurgeV1ControllerScheduledTest {
	private val userService: IUserService = mock()
	private val clock: Clock = Clock.fixed(Instant.parse("2026-10-12T10:00:00Z"), UTC)
	private val controller = PurgeV1Controller(
		userService, USERS_THRESHOLD,
		mock<IProjectService>(), 12,
		mock<IMovementService>(), mock<IAlertService>(), mock<ICommunicationService>(), 12,
		mock<IActivityService>(), mock<IVehicleService>(), mock<IParticipantService>(), mock<IGroupService>(),
		clock,
		12,
	)

	private companion object {
		private const val USERS_THRESHOLD = 6L
	}

	@Test
	fun `Should scheduledUsersPurge purge users older than the configured threshold`() {
		// Arrange
		whenever(userService.purgeUsersIfNecessary(eq(LocalDate.of(2026, 4, 12)), eq(false)))
			.thenReturn(Flux.just(UUID.randomUUID()))

		// Act
		controller.scheduledUsersPurge()

		// Assert
		verify(userService).purgeUsersIfNecessary(LocalDate.of(2026, 4, 12), false)
	}

	@Test
	fun `Should scheduledUsersPurge not propagate a purge failure`() {
		// Arrange
		whenever(userService.purgeUsersIfNecessary(eq(LocalDate.of(2026, 4, 12)), eq(false)))
			.thenReturn(Flux.error(IllegalStateException("boom")))

		// Act
		controller.scheduledUsersPurge()

		// Assert
		verify(userService).purgeUsersIfNecessary(LocalDate.of(2026, 4, 12), false)
	}
}
