package fr.laucoin.registry.backend.domain.helper

import fr.laucoin.registry.backend.domain.helper.PurgeHelper.purgeEach
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class PurgeHelperTest {
	private val first = UUID.randomUUID()
	private val second = UUID.randomUUID()
	private val deleted = ConcurrentLinkedQueue<UUID>()

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
		val result = ids.purgeEach("vehicle", dryRun = false) { Mono.error(failure) }

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
}
