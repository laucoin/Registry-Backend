package fr.laucoin.registry.backend.domain.helper

import fr.laucoin.registry.backend.domain.helper.PurgeHelper.purgeEach
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import reactor.test.scheduler.VirtualTimeScheduler

class PurgeHelperTest {
	private val first = UUID.randomUUID()
	private val second = UUID.randomUUID()
	private val deleted = ConcurrentLinkedQueue<UUID>()
	private val scheduler = VirtualTimeScheduler.create()

	private fun delete(id: UUID): Mono<Unit> = Mono.fromRunnable<Unit> { deleted.add(id) }.thenReturn(Unit)

	@Test
	fun `Should purgeEach emit every id without deleting in dry run`() {
		// Arrange
		val ids = Flux.just(first, second)

		// Act
		val result = ids.purgeEach("vehicle", dryRun = true, ::delete)

		// Assert
		StepVerifier.create(result.collectList())
			.assertNext { assertEquals(setOf(first, second), it.toSet()) }
			.verifyComplete()
		assertTrue(deleted.isEmpty())
	}

	@Test
	fun `Should purgeEach delete and emit every id outside dry run`() {
		// Arrange
		val ids = Flux.just(first, second)

		// Act
		val result = ids.purgeEach("vehicle", dryRun = false, ::delete)

		// Assert
		StepVerifier.create(result.collectList())
			.assertNext { assertEquals(setOf(first, second), it.toSet()) }
			.verifyComplete()
		assertEquals(setOf(first, second), deleted.toSet())
	}

	@Test
	fun `Should purgeEach propagate a deletion failure`() {
		// Arrange
		val failure = IllegalStateException("deletion failed")
		val ids = Flux.just(first)

		// Act
		val result = ids.purgeEach("vehicle", dryRun = false, delete = { Mono.error(failure) })

		// Assert
		StepVerifier.create(result)
			.expectErrorMatches { it === failure }
			.verify()
	}

	@Test
	fun `Should purgeEach complete empty when nothing is eligible`() {
		// Arrange
		val ids = Flux.empty<UUID>()

		// Act
		val result = ids.purgeEach("vehicle", dryRun = false, ::delete)

		// Assert
		StepVerifier.create(result).verifyComplete()
		assertTrue(deleted.isEmpty())
	}

	@Test
	fun `Should purgeEach delete one id at a time with a concurrency of one`() {
		// Arrange
		val inFlight = AtomicInteger(0)
		val maxInFlight = AtomicInteger(0)
		val ids = Flux.just(first, second, UUID.randomUUID())
		val delete = { _: UUID ->
			Mono.just(Unit)
				.doOnSubscribe { maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), ::maxOf) }
				.delayElement(Duration.ofMillis(10), scheduler)
				.doOnTerminate { inFlight.decrementAndGet() }
		}

		// Act
		val result = ids.purgeEach("user", dryRun = false, delete, concurrency = 1)

		// Assert
		StepVerifier.withVirtualTime({ result }, { scheduler }, Long.MAX_VALUE)
			.thenAwait(Duration.ofSeconds(1))
			.expectNextCount(3)
			.verifyComplete()
		assertEquals(1, maxInFlight.get())
	}
}
