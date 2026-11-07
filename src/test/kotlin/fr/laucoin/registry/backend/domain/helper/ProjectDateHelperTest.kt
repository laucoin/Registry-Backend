package fr.laucoin.registry.backend.domain.helper

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IProjectPort
import fr.laucoin.registry.backend.test.ModelExt.defaultProject
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZoneOffset
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT
import reactor.core.Exceptions
import reactor.core.publisher.Mono

class ProjectDateHelperTest {
	private val port: IProjectPort = mock()
	private val helper = ProjectDateHelper(port)

	private companion object {
		private val MIN_DATE = LocalDate.MIN
		private val MAX_DATE = LocalDate.MAX

		private val MIDNIGHT_PLUS_ONE_NANOSECOND = OffsetTime.of(0, 0, 0, 1, ZoneOffset.of("Z"))
		private val MIDNIGHT_LESS_ONE_NANOSECOND = OffsetTime.of(23, 59, 59, 0, ZoneOffset.of("Z"))

		@JvmStatic
		fun `Should validateDateTime call port findById and validate the request date is in project range`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(null, null, null),
				Arguments.of(null, null, CustomDateTimeModel(MIN_DATE)),
				Arguments.of(
					CustomDateTimeModel(MIN_DATE), CustomDateTimeModel(MAX_DATE), CustomDateTimeModel(MIN_DATE),
				),
				Arguments.of(
					CustomDateTimeModel(MIN_DATE), CustomDateTimeModel(MAX_DATE), CustomDateTimeModel(MAX_DATE),
				),
				Arguments.of(CustomDateTimeModel(MIN_DATE), CustomDateTimeModel(MAX_DATE), CustomDateTimeModel.MIN),
				Arguments.of(CustomDateTimeModel(MIN_DATE), CustomDateTimeModel(MAX_DATE), CustomDateTimeModel.MAX),
				Arguments.of(CustomDateTimeModel.MIN, CustomDateTimeModel.MAX, CustomDateTimeModel.MIN),
				Arguments.of(CustomDateTimeModel.MIN, CustomDateTimeModel.MAX, CustomDateTimeModel.MAX),
			)
		}

		@JvmStatic
		fun `Should validateDateTime call port findById and throw on invalid date`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(
					CustomDateTimeModel(MAX_DATE), CustomDateTimeModel(MAX_DATE), CustomDateTimeModel(MIN_DATE),
				),
				Arguments.of(
					CustomDateTimeModel(MIN_DATE), CustomDateTimeModel(MIN_DATE), CustomDateTimeModel(MAX_DATE),
				),
				Arguments.of(
					CustomDateTimeModel(MIN_DATE, MIDNIGHT_PLUS_ONE_NANOSECOND),
					CustomDateTimeModel(MAX_DATE),
					CustomDateTimeModel.MIN,
				),
				Arguments.of(
					CustomDateTimeModel(MIN_DATE),
					CustomDateTimeModel(MAX_DATE, MIDNIGHT_LESS_ONE_NANOSECOND),
					CustomDateTimeModel.MAX,
				),
			)
		}

		@JvmStatic
		fun `Should validateDateTimes call port findById and validate the request dates are in project range`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(null, null, null, null),
				Arguments.of(null, null, CustomDateTimeModel(MIN_DATE), CustomDateTimeModel(MAX_DATE)),
				Arguments.of(
					CustomDateTimeModel(MIN_DATE),
					CustomDateTimeModel(MAX_DATE),
					CustomDateTimeModel(MIN_DATE),
					CustomDateTimeModel(MAX_DATE),
				),
				Arguments.of(
					CustomDateTimeModel(MIN_DATE),
					CustomDateTimeModel(MAX_DATE),
					CustomDateTimeModel.MIN,
					CustomDateTimeModel(MIN_DATE, OffsetTime.MAX),
				),
				Arguments.of(
					CustomDateTimeModel.MIN, CustomDateTimeModel.MAX, CustomDateTimeModel.MIN, CustomDateTimeModel.MAX,
				),
			)
		}

		@JvmStatic
		fun `Should validateDateTimes call port findById and throw on invalid dates`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(
					CustomDateTimeModel(MAX_DATE),
					CustomDateTimeModel(MAX_DATE),
					CustomDateTimeModel(MIN_DATE),
					CustomDateTimeModel(MIN_DATE),
				),
				Arguments.of(
					CustomDateTimeModel(MIN_DATE),
					CustomDateTimeModel(MIN_DATE),
					CustomDateTimeModel(MAX_DATE),
					CustomDateTimeModel(MAX_DATE),
				),
				Arguments.of(
					CustomDateTimeModel(MIN_DATE, MIDNIGHT_PLUS_ONE_NANOSECOND),
					CustomDateTimeModel(MAX_DATE),
					CustomDateTimeModel.MIN,
					CustomDateTimeModel(MAX_DATE),
				),
				Arguments.of(
					CustomDateTimeModel(MIN_DATE),
					CustomDateTimeModel(MAX_DATE, MIDNIGHT_LESS_ONE_NANOSECOND),
					CustomDateTimeModel(MIN_DATE),
					CustomDateTimeModel.MAX,
				),
			)
		}
	}

	@ParameterizedTest
	@MethodSource
	fun `Should validateDateTime call port findById and validate the request date is in project range`(
		projectBeginDateTime: CustomDateTimeModel?,
		projectEndDateTime: CustomDateTimeModel?,
		dateTime: CustomDateTimeModel?,
	) {
		// Arrange
		val project = defaultProject().apply { begin = projectBeginDateTime?.copy(); end = projectEndDateTime?.copy() }
		val errorMessage = "ERROR_MESSAGE"

		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(project))

		// Act
		val result = helper.validateDateTime(defaultProjectId, dateTime?.copy(), errorMessage).block()

		// Assert
		assertEquals(defaultProjectId, result)

		verify(port).findById(defaultProjectId, isVisible = null)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should validateDateTime call port findById and throw on invalid date`(
		projectBeginDateTime: CustomDateTimeModel?,
		projectEndDateTime: CustomDateTimeModel?,
		dateTime: CustomDateTimeModel?,
	) {
		// Arrange
		val project = defaultProject().apply { begin = projectBeginDateTime?.copy(); end = projectEndDateTime?.copy() }
		val errorMessage = "ERROR_MESSAGE"

		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(project))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			helper.validateDateTime(defaultProjectId, dateTime?.copy(), errorMessage).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_CONTENT, result.status)
		assertEquals(errorMessage, result.message)
		assertEquals(3, result.args?.size)

		verify(port).findById(defaultProjectId, isVisible = null)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should validateDateTimes call port findById and validate the request dates are in project range`(
		projectBeginDateTime: CustomDateTimeModel?,
		projectEndDateTime: CustomDateTimeModel?,
		startDateTime: CustomDateTimeModel?,
		endDateTime: CustomDateTimeModel?,
	) {
		// Arrange
		val project = defaultProject().apply { begin = projectBeginDateTime; end = projectEndDateTime }
		val errorMessage = "ERROR_MESSAGE"

		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(project))

		// Act
		val result = helper.validateDateTimes(defaultProjectId, startDateTime, endDateTime, errorMessage).block()

		// Assert
		assertEquals(defaultProjectId, result)

		verify(port).findById(defaultProjectId, isVisible = null)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should validateDateTimes call port findById and throw on invalid dates`(
		projectBeginDateTime: CustomDateTimeModel?,
		projectEndDateTime: CustomDateTimeModel?,
		startDateTime: CustomDateTimeModel?,
		endDateTime: CustomDateTimeModel?,
	) {
		// Arrange
		val project = defaultProject().apply { begin = projectBeginDateTime?.copy(); end = projectEndDateTime?.copy() }
		val errorMessage = "ERROR_MESSAGE"

		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(project))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			helper.validateDateTimes(defaultProjectId, startDateTime?.copy(), endDateTime?.copy(), errorMessage)
				.block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_CONTENT, result.status)
		assertEquals(errorMessage, result.message)
		assertEquals(4, result.args?.size)

		verify(port).findById(defaultProjectId, isVisible = null)
	}
}
