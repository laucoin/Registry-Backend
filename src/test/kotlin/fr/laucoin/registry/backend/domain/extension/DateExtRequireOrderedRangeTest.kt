package fr.laucoin.registry.backend.domain.extension

import fr.laucoin.registry.backend.domain.constant.ErrorConst.DATE_RANGE_FROM_LATER_THAN_TO
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.ZonedDateTime.parse
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpStatus.BAD_REQUEST

class DateExtRequireOrderedRangeTest {
	companion object {
		private val early = parse("2024-01-01T00:00:00Z")
		private val late = parse("2024-02-01T00:00:00Z")

		@JvmStatic
		fun `Should accept ordered or open date ranges`(): Stream<Arguments> = Stream.of(
			Arguments.of(null, null),
			Arguments.of(LocalDate.of(2024, 1, 1), null),
			Arguments.of(null, LocalDate.of(2024, 2, 1)),
			Arguments.of(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1)),
			Arguments.of(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)),
		)

		@JvmStatic
		fun `Should accept ordered or open ranges`(): Stream<Arguments> = Stream.of(
			Arguments.of(null, null),
			Arguments.of(early, null),
			Arguments.of(null, late),
			Arguments.of(early, late),
			Arguments.of(early, early),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should accept ordered or open ranges`(from: ZonedDateTime?, to: ZonedDateTime?) {
		// Act & Assert
		assertDoesNotThrow { DateExt.requireOrderedRange(from, to) }
	}

	@Test
	fun `Should reject inverted range`() {
		// Act
		val exception = assertThrows(RegistryException::class.java) { DateExt.requireOrderedRange(late, early) }

		// Assert
		assertEquals(BAD_REQUEST, exception.status)
		assertEquals(DATE_RANGE_FROM_LATER_THAN_TO, exception.code)
	}

	@Test
	fun `Should reject inverted range when building search params`() {
		// Act & Assert
		assertThrows(RegistryException::class.java) { AlertSearchParamModel(query = null, from = late, to = early) }
		assertThrows(RegistryException::class.java) { CommunicationSearchParamModel(query = null, from = late, to = early) }
		assertThrows(RegistryException::class.java) { MovementSearchParamModel(type = null, from = late, to = early) }
	}

	@ParameterizedTest
	@MethodSource
	fun `Should accept ordered or open date ranges`(from: LocalDate?, to: LocalDate?) {
		// Act & Assert
		assertDoesNotThrow { DateExt.requireOrderedRange(from, to) }
	}

	@Test
	fun `Should reject an inverted date range`() {
		// Arrange
		val from = LocalDate.of(2024, 2, 1)
		val to = LocalDate.of(2024, 1, 1)

		// Act
		val exception = assertThrows(RegistryException::class.java) { DateExt.requireOrderedRange(from, to) }

		// Assert
		assertEquals(BAD_REQUEST, exception.status)
		assertEquals(DATE_RANGE_FROM_LATER_THAN_TO, exception.code)
	}
}
