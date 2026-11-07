package fr.laucoin.registry.backend.infrastructure.driving.api.helper

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_DURATION_FORMAT_FAILED
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.DurationFilterHelper.parseDurationFilter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.days

class DurationFilterHelperTest {
	@Test
	fun `Should parseDurationFilter accept the absence of filter and the one year bound`() {
		// Act
		val absent = parseDurationFilter(null)
		val bound = parseDurationFilter("P366D")

		// Assert
		assertNull(absent)
		assertEquals(366.days, bound)
	}

	@ParameterizedTest
	@ValueSource(strings = ["P367D", "-PT1H", "two hours"])
	fun `Should parseDurationFilter refuse a duration over a year, negative or malformed`(duration: String) {
		// Act
		val result = assertThrows(RegistryException::class.java) { parseDurationFilter(duration) }

		// Assert
		assertEquals(ACTIVITY_DURATION_FORMAT_FAILED, result.code)
	}
}
