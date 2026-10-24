package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.RESOURCE_MODIFIED_SINCE_READ
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum.ACTIVITY
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IRowLockPort
import java.time.Instant
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.PRECONDITION_FAILED
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class ConcurrencyServiceTest {
	private val rowLockPort: IRowLockPort = mock()
	private val transactionalOperator: TransactionalOperator = mock()
	private val service = ConcurrencyService(rowLockPort, transactionalOperator)
	private val id: UUID = UUID.randomUUID()
	private val edition: Instant = Instant.parse("2026-10-13T10:02:11.123456Z")

	@BeforeEach
	fun setUp() {
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<Mono<*>>(0) }
		whenever(rowLockPort.lock(any(), any())).thenReturn(Mono.just(Unit))
	}

	private fun model() = ActivityModel().apply {
		lastEdition = HistoryModel(dateTime = ZonedDateTime.ofInstant(edition, UTC))
	}

	@Test
	fun `Should run the action without locking when no precondition is expected`() {
		// Act
		val result = service.updateIfUnchanged(ACTIVITY, id, null, { Mono.just(model()) }, { Mono.just("done") })

		// Assert
		StepVerifier.create(result).expectNext("done").verifyComplete()
		verify(rowLockPort, never()).lock(any(), any())
	}

	@Test
	fun `Should lock the row then run the action when the last edition matches at the millisecond`() {
		// Arrange
		val expected = listOf(Instant.ofEpochMilli(1), Instant.ofEpochMilli(edition.toEpochMilli()))

		// Act
		val result = service.updateIfUnchanged(ACTIVITY, id, expected, { Mono.just(model()) }, { Mono.just("done") })

		// Assert
		StepVerifier.create(result).expectNext("done").verifyComplete()
		verify(rowLockPort).lock(ACTIVITY, id)
	}

	@Test
	fun `Should reject the action when the resource changed since it was read`() {
		// Act
		val result = service.updateIfUnchanged(
			ACTIVITY, id, listOf(Instant.ofEpochMilli(1)), { Mono.just(model()) }, { Mono.just("done") },
		)

		// Assert
		StepVerifier.create(result)
			.expectErrorSatisfies {
				assertEquals(PRECONDITION_FAILED, (it as RegistryException).status)
				assertEquals(RESOURCE_MODIFIED_SINCE_READ, it.code)
			}
			.verify()
	}

	@Test
	fun `Should reject every expected edition when none is parsable`() {
		// Act
		val result = service.updateIfUnchanged(ACTIVITY, id, emptyList(), { Mono.just(model()) }, { Mono.just("done") })

		// Assert
		StepVerifier.create(result).expectError(RegistryException::class.java).verify()
	}

	@Test
	fun `Should let the action report a missing resource`() {
		// Act
		val result = service.updateIfUnchanged(
			ACTIVITY,
			id,
			listOf(Instant.ofEpochMilli(1)),
			{ Mono.empty<ActivityModel>() },
			{ Mono.error<String>(IllegalStateException("not found")) },
		)

		// Assert
		StepVerifier.create(result).expectError(IllegalStateException::class.java).verify()
	}
}
