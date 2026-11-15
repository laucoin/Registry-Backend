package fr.laucoin.registry.backend.domain.constant

import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.DURATION_PATTERN
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.ROLE_PATTERN
import java.util.stream.Stream
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

class ValidationLimitsConstTest {
	private companion object {
		@JvmStatic
		fun `Should the role pattern accept only letters, digits and underscores`(): Stream<Arguments> = Stream.of(
			arguments("PROJECT_ADMINISTRATOR", true),
			arguments("project_admin2", true),
			arguments("", true),
			arguments("ROLE';--", false),
			arguments("ROLE-ADMIN", false),
			arguments("ROLE ADMIN", false),
			arguments(" ROLE", false),
			arguments("ROLE ", false),
			arguments("ROLE\n", false),
			arguments("R\u00F4le", false),
		)

		@JvmStatic
		fun `Should the duration pattern accept every duration notation and nothing else`(): Stream<Arguments> = Stream.of(
			arguments("PT1H30M", true),
			arguments("P1DT2H", true),
			arguments("PT1.5H", true),
			arguments("PT0,5S", true),
			arguments("-PT1H", true),
			arguments("+PT1H", true),
			arguments("1h 30m", true),
			arguments("PT1H;DROP", false),
			arguments("PT1H<x>", false),
			arguments("PT1H'--", false),
			arguments("PT1H\n", false),
			arguments("PT1H\r", false),
			arguments("\tPT1H", false),
		)
	}

	@ParameterizedTest
	@MethodSource("Should the role pattern accept only letters, digits and underscores")
	fun `Should the role pattern accept only letters, digits and underscores`(value: String, expected: Boolean) {
		// Act
		val result = Regex(ROLE_PATTERN).matches(value)

		// Assert
		assertEquals(expected, result)
	}

	@ParameterizedTest
	@MethodSource("Should the duration pattern accept every duration notation and nothing else")
	fun `Should the duration pattern accept every duration notation and nothing else`(value: String, expected: Boolean) {
		// Act
		val result = Regex(DURATION_PATTERN).matches(value)

		// Assert
		assertEquals(expected, result)
	}
}
