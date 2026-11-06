package fr.laucoin.registry.backend.infrastructure.driving.security.service

import fr.laucoin.registry.backend.test.MutableClock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SlidingWindowCounterTest {
	private companion object {
		val START: Instant = Instant.parse("2030-01-01T00:00:00Z")
		val WINDOW: Duration = Duration.ofSeconds(60)
		const val THREADS = 16
		const val CALLS_PER_THREAD = 50
	}

	private val clock = MutableClock(START)

	@Test
	fun `Should tryConsume accept exactly capacity calls then refuse the next one`() {
		// Arrange
		val counter = SlidingWindowCounter(clock, 3, WINDOW)

		// Act
		val results = List(4) { counter.tryConsume() }

		// Assert
		assertEquals(listOf(true, true, true, false), results)
	}

	@Test
	fun `Should tryConsume keep refusing until the window has fully elapsed`() {
		// Arrange
		val counter = SlidingWindowCounter(clock, 1, WINDOW)
		counter.tryConsume()

		// Act
		clock.advance(WINDOW.minusMillis(1))
		val beforeEnd = counter.tryConsume()
		clock.advance(Duration.ofMillis(1))
		val atEnd = counter.tryConsume()

		// Assert
		assertFalse(beforeEnd)
		assertTrue(atEnd)
	}

	@Test
	fun `Should tryConsume never accept more than capacity under concurrent calls`() {
		// Arrange
		val capacity = 100
		val counter = SlidingWindowCounter(clock, capacity, WINDOW)
		val executor = Executors.newFixedThreadPool(THREADS)
		val start = CountDownLatch(1)
		val futures = List(THREADS) {
			executor.submit<Int> {
				start.await()
				(1..CALLS_PER_THREAD).count { counter.tryConsume() }
			}
		}

		// Act
		start.countDown()
		val accepted = futures.sumOf { it.get(10, TimeUnit.SECONDS) }
		executor.shutdown()

		// Assert
		assertEquals(capacity, accepted)
	}
}
