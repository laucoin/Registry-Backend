package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.annotation.StartBeforeEnd
import fr.laucoin.registry.backend.domain.constant.ErrorConst.COMPARING_WRONG_PARAMETER_TYPE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NO_PARAMETER_FOUND_FOR_SPECIFIED_NAME
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.CustomDateTimeWriterDto
import jakarta.validation.ConstraintValidatorContext
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR

class StartBeforeEndValidatorTest {
	private val dateValidator = StartBeforeEndValidator()

	private companion object {
		private val yesterdayDateTime: ZonedDateTime = ZonedDateTime.now().minusDays(1)
		private val nowDateTime: ZonedDateTime = ZonedDateTime.now()
		private val tomorrowDateTime: ZonedDateTime = ZonedDateTime.now().plusDays(1)

		private val yesterdayDate: LocalDate = LocalDate.now().minusDays(1)
		private val nowDate: LocalDate = LocalDate.now()
		private val tomorrowDate: LocalDate = LocalDate.now().plusDays(1)

		@JvmStatic
		fun `Should isValid throw for no-existing specified field`(): Stream<Arguments> = Stream.of(
			Arguments.of("test", "end"),
			Arguments.of("start", "test"),
			Arguments.of("test", "test"),
		)

		@JvmStatic
		fun `Should isValid validate ZonedDateTime`(): Stream<Arguments> = Stream.of(
			Arguments.of(null, null, true),
			Arguments.of(null, nowDateTime, true),
			Arguments.of(nowDateTime, null, true),
			Arguments.of(nowDateTime, nowDateTime, false),
			Arguments.of(tomorrowDateTime, nowDateTime, false),
			Arguments.of(yesterdayDateTime, nowDateTime, true),
			Arguments.of(nowDateTime, yesterdayDateTime, false),
			Arguments.of(nowDateTime, tomorrowDateTime, true),
		)

		@JvmStatic
		fun `Should isValid validate LocalDate`(): Stream<Arguments> = Stream.of(
			Arguments.of(null, null, true),
			Arguments.of(null, nowDate, true),
			Arguments.of(nowDate, null, true),
			Arguments.of(nowDate, nowDate, false),
			Arguments.of(tomorrowDate, nowDate, false),
			Arguments.of(yesterdayDate, nowDate, true),
			Arguments.of(nowDate, yesterdayDate, false),
			Arguments.of(nowDate, tomorrowDate, true),
		)

		@JvmStatic
		fun `Should isValid validate CustomDateTimeWriterDto`(): Stream<Arguments> = Stream.of(
			Arguments.of(null, null, true),
			Arguments.of(null, CustomDateTimeWriterDto(nowDate), true),
			Arguments.of(CustomDateTimeWriterDto(nowDate), null, true),
			Arguments.of(CustomDateTimeWriterDto(nowDate), CustomDateTimeWriterDto(nowDate), true),
			Arguments.of(CustomDateTimeWriterDto(tomorrowDate), CustomDateTimeWriterDto(nowDate), false),
			Arguments.of(CustomDateTimeWriterDto(yesterdayDate), CustomDateTimeWriterDto(nowDate), true),
			Arguments.of(CustomDateTimeWriterDto(nowDate), CustomDateTimeWriterDto(yesterdayDate), false),
			Arguments.of(CustomDateTimeWriterDto(nowDate), CustomDateTimeWriterDto(tomorrowDate), true),
			Arguments.of(
				CustomDateTimeWriterDto(nowDate, nowDateTime.toOffsetDateTime().toOffsetTime()),
				CustomDateTimeWriterDto(nowDate),
				true
			),
			Arguments.of(
				CustomDateTimeWriterDto(nowDate, nowDateTime.toOffsetDateTime().toOffsetTime()),
				CustomDateTimeWriterDto(nowDate, nowDateTime.toOffsetDateTime().toOffsetTime()),
				false
			),
			Arguments.of(
				CustomDateTimeWriterDto(tomorrowDate, tomorrowDateTime.toOffsetDateTime().toOffsetTime()),
				CustomDateTimeWriterDto(nowDate, nowDateTime.toOffsetDateTime().toOffsetTime()),
				false
			),
			Arguments.of(
				CustomDateTimeWriterDto(yesterdayDate, yesterdayDateTime.toOffsetDateTime().toOffsetTime()),
				CustomDateTimeWriterDto(nowDate, nowDateTime.toOffsetDateTime().toOffsetTime()),
				true
			),
			Arguments.of(
				CustomDateTimeWriterDto(nowDate, nowDateTime.toOffsetDateTime().toOffsetTime()),
				CustomDateTimeWriterDto(yesterdayDate, yesterdayDateTime.toOffsetDateTime().toOffsetTime()),
				false
			),
			Arguments.of(
				CustomDateTimeWriterDto(nowDate, nowDateTime.toOffsetDateTime().toOffsetTime()),
				CustomDateTimeWriterDto(tomorrowDate, tomorrowDateTime.toOffsetDateTime().toOffsetTime()),
				true
			),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should isValid throw for no-existing specified field`(
		startField: String,
		endField: String,
	) {
		// Arrange
		val context: ConstraintValidatorContext = mock()
		val data = ClassWithDate(startZonedDateTime = ZonedDateTime.now(), endZonedDateTime = ZonedDateTime.now())
		dateValidator.initialize(StartBeforeEnd(startField = startField, endField = endField, message = "TEST_MESSAGE"))

		// Act
		val result = assertThrows(RegistryException::class.java) {
			dateValidator.isValid(data, context)
		}

		// Assert
		assertEquals(INTERNAL_SERVER_ERROR, result.status)
		assertEquals(NO_PARAMETER_FOUND_FOR_SPECIFIED_NAME, result.message)
	}

	@Test
	fun `Should isValid throw for different field type`() {
		// Arrange
		val context: ConstraintValidatorContext = mock()
		val data = ClassWithDate(startZonedDateTime = ZonedDateTime.now(), endLocalDate = LocalDate.now())
		dateValidator.initialize(
			StartBeforeEnd(
				startField = "startZonedDateTime",
				endField = "endLocalDate",
				message = "TEST_MESSAGE"
			)
		)

		// Act
		val result = assertThrows(RegistryException::class.java) {
			dateValidator.isValid(data, context)
		}

		// Assert
		assertEquals(INTERNAL_SERVER_ERROR, result.status)
		assertEquals(COMPARING_WRONG_PARAMETER_TYPE, result.message)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should isValid validate ZonedDateTime`(
		start: ZonedDateTime?,
		end: ZonedDateTime?,
		expected: Boolean,
	) {
		// Arrange
		val context: ConstraintValidatorContext = mock()
		val data = ClassWithDate(startZonedDateTime = start, endZonedDateTime = end)
		dateValidator.initialize(
			StartBeforeEnd(
				startField = "startZonedDateTime",
				endField = "endZonedDateTime",
				message = "TEST_MESSAGE"
			)
		)

		// Act
		val result = dateValidator.isValid(data, context)

		// Assert
		assertEquals(expected, result)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should isValid validate LocalDate`(
		start: LocalDate?,
		end: LocalDate?,
		expected: Boolean,
	) {
		// Arrange
		val context: ConstraintValidatorContext = mock()
		val data = ClassWithDate(startLocalDate = start, endLocalDate = end)
		dateValidator.initialize(
			StartBeforeEnd(
				startField = "startLocalDate",
				endField = "endLocalDate",
				message = "TEST_MESSAGE"
			)
		)

		// Act
		val result = dateValidator.isValid(data, context)

		// Assert
		assertEquals(expected, result)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should isValid validate CustomDateTimeWriterDto`(
		start: CustomDateTimeWriterDto?,
		end: CustomDateTimeWriterDto?,
		expected: Boolean,
	) {
		// Arrange
		val context: ConstraintValidatorContext = mock()
		val data = ClassWithDate(startCustomDateTime = start, endCustomDateTime = end)
		dateValidator.initialize(
			StartBeforeEnd(
				startField = "startCustomDateTime",
				endField = "endCustomDateTime",
				message = "TEST_MESSAGE"
			)
		)

		// Act
		val result = dateValidator.isValid(data, context)

		// Assert
		assertEquals(expected, result)
	}

	data class ClassWithDate(
		val startZonedDateTime: ZonedDateTime? = null,
		val endZonedDateTime: ZonedDateTime? = null,
		val startLocalDate: LocalDate? = null,
		val endLocalDate: LocalDate? = null,
		val startCustomDateTime: CustomDateTimeWriterDto? = null,
		val endCustomDateTime: CustomDateTimeWriterDto? = null,
	)
}
