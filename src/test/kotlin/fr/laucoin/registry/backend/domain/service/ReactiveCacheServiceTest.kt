package fr.laucoin.registry.backend.domain.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

class ReactiveCacheServiceTest {
	private val cache = ReactiveCacheService<String, Int>(ttl = Duration.ofMinutes(5))

	@Test
	fun `Should load once and serve subsequent reads from memory`() {
		// Arrange
		val loads = AtomicInteger()
		val loader = { _: String -> Mono.fromCallable { loads.incrementAndGet() } }

		// Act + Assert
		StepVerifier.create(cache.get("key", loader)).expectNext(1).verifyComplete()
		StepVerifier.create(cache.get("key", loader)).expectNext(1).verifyComplete()
		StepVerifier.create(cache.get("other", loader)).expectNext(2).verifyComplete()
		assertEquals(2, loads.get())
	}

	@Test
	fun `Should reload after an explicit eviction`() {
		// Arrange
		val loads = AtomicInteger()
		val loader = { _: String -> Mono.fromCallable { loads.incrementAndGet() } }
		StepVerifier.create(cache.get("key", loader)).expectNext(1).verifyComplete()

		// Act
		cache.evictAll()

		// Assert
		StepVerifier.create(cache.get("key", loader)).expectNext(2).verifyComplete()
	}

	@Test
	fun `Should never cache a failed load`() {
		// Arrange
		val loads = AtomicInteger()
		val failingOnce = { _: String ->
			Mono.defer {
				if (loads.incrementAndGet() == 1) Mono.error(IllegalStateException("boom"))
				else Mono.just(42)
			}
		}

		// Act + Assert
		assertThrows<IllegalStateException> { cache.get("key", failingOnce).block() }
		StepVerifier.create(cache.get("key", failingOnce)).expectNext(42).verifyComplete()
	}
}
