package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.enumeration.AuditActionEnum
import fr.laucoin.registry.backend.domain.enumeration.AuditOutcomeEnum
import fr.laucoin.registry.backend.domain.enumeration.AuditResourceTypeEnum
import fr.laucoin.registry.backend.domain.model.AuditModel
import fr.laucoin.registry.backend.domain.model.ClientContextModel
import fr.laucoin.registry.backend.domain.port.IAuditPort
import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono

class AuditServiceTest {
	private val port: IAuditPort = mock()

	private fun audit(outcome: AuditOutcomeEnum = AuditOutcomeEnum.SUCCESS) = AuditModel(
		action = AuditActionEnum.EXPORT,
		resourceType = AuditResourceTypeEnum.USER,
		resourceId = UUID.randomUUID(),
		projectId = null,
		actorUserId = UUID.randomUUID(),
		outcome = outcome,
		client = ClientContextModel("127.0.0.1", "JUnit"),
		occurredAt = ZonedDateTime.parse("2026-01-01T10:00:00Z"),
	)

	@Test
	fun `Should record write the event through the port once started`() {
		// Arrange
		val event = audit()
		whenever(port.record(any())).thenReturn(Mono.just(Unit))
		val service = AuditService(port, 10).apply { start() }

		// Act
		service.record(event)

		// Assert
		verify(port, timeout(WAIT_MS)).record(event)
	}

	@Test
	fun `Should record keep writing after the port failed`() {
		// Arrange
		val failing = audit(AuditOutcomeEnum.FORBIDDEN)
		val next = audit()
		whenever(port.record(failing)).thenReturn(Mono.error(IllegalStateException("db down")))
		whenever(port.record(next)).thenReturn(Mono.just(Unit))
		val service = AuditService(port, 10).apply { start() }

		// Act
		service.record(failing)
		service.record(next)

		// Assert
		verify(port, timeout(WAIT_MS)).record(next)
	}

	@Test
	fun `Should record drop the event when the queue is full`() {
		// Arrange
		val kept = audit()
		val dropped = audit(AuditOutcomeEnum.RATE_LIMITED)
		whenever(port.record(any())).thenReturn(Mono.just(Unit))
		val service = AuditService(port, 1)

		// Act
		service.record(kept)
		service.record(dropped)
		service.start()

		// Assert
		verify(port, timeout(WAIT_MS)).record(kept)
		verify(port, never()).record(dropped)
	}

	@Test
	fun `Should stop drain the queued events before calling back`() {
		// Arrange
		val events = List(3) { audit() }
		whenever(port.record(any())).thenReturn(Mono.just(Unit))
		val service = AuditService(port, 10)
		events.forEach(service::record)
		service.start()
		val stopped = CountDownLatch(1)

		// Act
		service.stop(stopped::countDown)

		// Assert
		assertTrue(stopped.await(WAIT_MS, TimeUnit.MILLISECONDS))
		events.forEach { verify(port).record(it) }
		assertFalse(service.isRunning)
	}

	@Test
	fun `Should stop call back immediately when never started`() {
		// Arrange
		val stopped = CountDownLatch(1)

		// Act
		AuditService(port, 10).stop(stopped::countDown)

		// Assert
		assertTrue(stopped.await(WAIT_MS, TimeUnit.MILLISECONDS))
	}

	private companion object {
		const val WAIT_MS = 2000L
	}
}
