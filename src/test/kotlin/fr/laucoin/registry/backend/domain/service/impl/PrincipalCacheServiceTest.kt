package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

class PrincipalCacheServiceTest {
	private val service = PrincipalCacheService(TTL_SECONDS)

	@Test
	fun `Should get load the principal once then serve it from the cache`() {
		// Arrange
		val oidcId = UUID.randomUUID()
		val loads = AtomicInteger()
		val loader = { Mono.fromSupplier { loads.incrementAndGet(); CurrentUserModel() } }

		// Act
		val first = service.get(oidcId, loader).block()
		val second = service.get(oidcId, loader).block()

		// Assert
		assertEquals(1, loads.get())
		assertEquals(first, second)
	}

	@Test
	fun `Should invalidate force the next get to reload the principal`() {
		// Arrange
		val oidcId = UUID.randomUUID()
		val loads = AtomicInteger()
		val loader = { Mono.fromSupplier { loads.incrementAndGet(); CurrentUserModel() } }
		service.get(oidcId, loader).block()

		// Act
		service.invalidate(oidcId)
		service.get(oidcId, loader).block()

		// Assert
		assertEquals(2, loads.get())
	}

	@Test
	fun `Should invalidateAll reload every given principal`() {
		// Arrange
		val ids = listOf(UUID.randomUUID(), UUID.randomUUID())
		val loads = AtomicInteger()
		val loader = { Mono.fromSupplier { loads.incrementAndGet(); CurrentUserModel() } }
		ids.forEach { service.get(it, loader).block() }

		// Act
		service.invalidateAll(ids)
		ids.forEach { service.get(it, loader).block() }

		// Assert
		assertEquals(4, loads.get())
	}

	@Test
	fun `Should get fail and not cache anything when the loader completes without a value`() {
		// Arrange
		val oidcId = UUID.randomUUID()

		// Act
		val result = service.get(oidcId) { Mono.empty() }

		// Assert
		StepVerifier.create(result).expectError(IllegalStateException::class.java).verify()
	}

	@Test
	fun `Should get propagate the loader error and retry on the next call`() {
		// Arrange
		val oidcId = UUID.randomUUID()
		service.get(oidcId) { Mono.error(IllegalArgumentException("boom")) }.onErrorResume { Mono.empty() }.block()

		// Act
		val principal = service.get(oidcId) { Mono.just(CurrentUserModel()) }.block()

		// Assert
		assertEquals(CurrentUserModel(), principal)
	}

	@Test
	fun `Should invalidate ignore a missing identifier`() {
		// Act
		val action = { service.invalidate(null) }

		// Assert
		assertDoesNotThrow(action)
	}

	private companion object {
		const val TTL_SECONDS = 60L
	}
}
