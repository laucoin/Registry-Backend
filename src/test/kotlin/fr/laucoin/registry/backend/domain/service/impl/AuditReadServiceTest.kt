package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.enumeration.AuditActionEnum
import fr.laucoin.registry.backend.domain.enumeration.AuditOutcomeEnum
import fr.laucoin.registry.backend.domain.enumeration.AuditResourceTypeEnum
import fr.laucoin.registry.backend.domain.model.AuditModel
import fr.laucoin.registry.backend.domain.model.ClientContextModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.service.IAuditService
import fr.laucoin.registry.backend.test.MutableClock
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class AuditReadServiceTest {
	private val auditService: IAuditService = mock()
	private val clock = MutableClock(Instant.parse("2026-01-01T10:00:00Z"))
	private val service = AuditReadService(auditService, clock, Duration.ofSeconds(60))
	private val client = ClientContextModel("127.0.0.1", "JUnit")
	private val projectId = UUID.randomUUID()

	private fun user() = CurrentUserModel().apply { id = UUID.randomUUID() }

	private fun recordList(user: CurrentUserModel, outcome: AuditOutcomeEnum = AuditOutcomeEnum.SUCCESS) =
		service.recordRead(user, AuditResourceTypeEnum.PARTICIPANT, null, projectId, client, outcome)

	@Test
	fun `Should recordRead write a READ event with the actor, the resource and the current time`() {
		// Arrange
		val user = user()
		val resourceId = UUID.randomUUID()
		val captor = argumentCaptor<AuditModel>()

		// Act
		service.recordRead(user, AuditResourceTypeEnum.MOVEMENT, resourceId, projectId, client)

		// Assert
		verify(auditService).record(captor.capture())
		assertEquals(
			AuditModel(
				AuditActionEnum.READ, AuditResourceTypeEnum.MOVEMENT, resourceId, projectId, user.id,
				AuditOutcomeEnum.SUCCESS, client, ZonedDateTime.parse("2026-01-01T10:00:00Z"),
			),
			captor.firstValue,
		)
	}

	@Test
	fun `Should recordRead trace every read of a single resource`() {
		// Arrange
		val user = user()
		val resourceId = UUID.randomUUID()

		// Act
		repeat(3) { service.recordRead(user, AuditResourceTypeEnum.USER, resourceId, null, client) }

		// Assert
		verify(auditService, times(3)).record(any())
	}

	@Test
	fun `Should recordRead trace a list read once within the window`() {
		// Arrange
		val user = user()

		// Act
		repeat(3) { recordList(user) }

		// Assert
		verify(auditService, times(1)).record(any())
	}

	@Test
	fun `Should recordRead trace a list read again once the window has elapsed`() {
		// Arrange
		val user = user()
		recordList(user)

		// Act
		clock.advance(Duration.ofSeconds(61))
		recordList(user)

		// Assert
		verify(auditService, times(2)).record(any())
	}

	@Test
	fun `Should recordRead trace refused reads of one actor once whatever the resource or project`() {
		// Arrange
		val user = user()

		// Act
		repeat(3) {
			service.recordRead(user, AuditResourceTypeEnum.PARTICIPANT, UUID.randomUUID(), UUID.randomUUID(), client, AuditOutcomeEnum.FORBIDDEN)
		}
		service.recordRead(user, AuditResourceTypeEnum.USER, UUID.randomUUID(), null, client, AuditOutcomeEnum.FORBIDDEN)

		// Assert
		verify(auditService, times(2)).record(any())
	}

	@Test
	fun `Should recordRead trace list reads of another actor, project or outcome separately`() {
		// Arrange
		val user = user()

		// Act
		recordList(user)
		recordList(user())
		recordList(user, AuditOutcomeEnum.FORBIDDEN)
		service.recordRead(user, AuditResourceTypeEnum.PARTICIPANT, null, UUID.randomUUID(), client)

		// Assert
		verify(auditService, times(4)).record(any())
	}
}
