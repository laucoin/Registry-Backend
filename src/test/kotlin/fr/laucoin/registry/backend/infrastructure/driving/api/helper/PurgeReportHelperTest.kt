package fr.laucoin.registry.backend.infrastructure.driving.api.helper

import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PURGE_REPORTED_IDS_MAX
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.PurgeReportHelper.reportedOnceCompleted
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

class PurgeReportHelperTest {
	@Test
	fun `Should reportedOnceCompleted consume the whole purge but report at most the configured identifiers`() {
		// Arrange
		val total = PURGE_REPORTED_IDS_MAX + 5
		val consumed = AtomicInteger(0)
		val purge = Flux.range(0, total).map { UUID.randomUUID() }.doOnNext { consumed.incrementAndGet() }

		// Act
		val result = purge.reportedOnceCompleted()

		// Assert
		StepVerifier.create(result).expectNextCount(PURGE_REPORTED_IDS_MAX.toLong()).verifyComplete()
		assertEquals(total, consumed.get())
	}
}
