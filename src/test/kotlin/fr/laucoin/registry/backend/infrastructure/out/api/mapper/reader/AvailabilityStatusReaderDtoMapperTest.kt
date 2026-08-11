package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.AVAILABILITY_STATUS_DURATION_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.AVAILABILITY_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum.AVAILABLE
import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum.UNAVAILABLE
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.stream.Stream
import kotlin.test.assertEquals

class AvailabilityStatusReaderDtoMapperTest {
	private val translateService: ITranslateService = mock()
	private val mapper = AvailabilityStatusReaderDtoMapper(translateService)

	private companion object {
		@JvmStatic
		fun `Should not claim a still-open window has expired`(): Stream<Arguments> = Stream.of(
			Arguments.of(
				CustomDateTimeModel(LocalDate.now().minusDays(3)),
				CustomDateTimeModel(LocalDate.now().plusDays(3)),
				"$AVAILABILITY_STATUS_PREFIX$UNAVAILABLE",
			),
			Arguments.of(
				null,
				CustomDateTimeModel(LocalDate.now().plusDays(30)),
				"$AVAILABILITY_STATUS_PREFIX$UNAVAILABLE",
			),
			Arguments.of(
				CustomDateTimeModel(LocalDate.now().plusDays(30)),
				CustomDateTimeModel(LocalDate.now().plusDays(60)),
				"${AVAILABILITY_STATUS_DURATION_PREFIX}NOT_YET",
			),
			Arguments.of(
				CustomDateTimeModel(LocalDate.now().minusDays(60)),
				CustomDateTimeModel(LocalDate.now().minusDays(30)),
				"${AVAILABILITY_STATUS_DURATION_PREFIX}NO_MORE",
			),
		)
	}

	@BeforeEach
	fun setUp() {
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull()))
			.thenAnswer { it.arguments[0] }
	}

	/**
	 * Regression: a null window used to route into Duration.between(…, null)
	 * and crash every response containing the entity (e.g. the creator
	 * profile of a freshly created project).
	 */
	@Test
	fun `Should fall back to the plain status label when the availability window is null`() {
		// Act
		val result = mapper.toDto(AVAILABLE, startAvailability = null, endAvailability = null)

		// Assert
		assertEquals(AVAILABLE.name, result.value)
		assertEquals("$AVAILABILITY_STATUS_PREFIX$AVAILABLE", result.label)
	}

	@Test
	fun `Should fall back to the plain status label for an unavailable entity without window`() {
		// Act
		val result = mapper.toDto(UNAVAILABLE, startAvailability = null, endAvailability = null)

		// Assert
		assertEquals(UNAVAILABLE.name, result.value)
		assertEquals("$AVAILABILITY_STATUS_PREFIX$UNAVAILABLE", result.label)
	}

	@Test
	fun `Should compute the elapsed duration for an available entity with a start date`() {
		// Arrange
		val start = CustomDateTimeModel(LocalDate.now().minusDays(3))

		// Act
		val result = mapper.toDto(AVAILABLE, startAvailability = start, endAvailability = null)

		// Assert
		assertEquals("$AVAILABILITY_STATUS_DURATION_PREFIX$AVAILABLE", result.label)
	}

	/**
	 * The direction question the v1 mapper left open, now settled: NOT_YET means
	 * the window has not OPENED and NO_MORE that it has CLOSED. Both branches
	 * used to test the opposite, which is how an availability ending in three
	 * days announced itself as having ended a few seconds ago.
	 */
	@Test
	fun `Should announce the not-yet label when the window has not opened`() {
		// Arrange
		val start = CustomDateTimeModel(LocalDate.now().plusDays(3))

		// Act
		val result = mapper.toDto(UNAVAILABLE, startAvailability = start, endAvailability = null)

		// Assert
		assertEquals("${AVAILABILITY_STATUS_DURATION_PREFIX}NOT_YET", result.label)
	}

	@Test
	fun `Should announce the no-more label when the window has closed`() {
		// Arrange
		val end = CustomDateTimeModel(LocalDate.now().minusDays(3))

		// Act
		val result = mapper.toDto(UNAVAILABLE, startAvailability = null, endAvailability = end)

		// Assert
		assertEquals("${AVAILABILITY_STATUS_DURATION_PREFIX}NO_MORE", result.label)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should not claim a still-open window has expired`(
		startAvailability: CustomDateTimeModel?,
		endAvailability: CustomDateTimeModel?,
		expected: String,
	) {
		// Act
		val result = mapper.toDto(UNAVAILABLE, startAvailability, endAvailability)

		// Assert
		assertEquals(expected, result.label)
	}

	/**
	 * A bare end date closes at 23:59:59, so an availability ending TODAY is
	 * still open — the boundary is what keeps the label honest for the whole of
	 * the last day.
	 */
	@Test
	fun `Should keep an availability ending today open until its last second`() {
		// Arrange
		val end = CustomDateTimeModel(LocalDate.now())

		// Act
		val result = mapper.toDto(UNAVAILABLE, startAvailability = null, endAvailability = end)

		// Assert
		assertEquals("$AVAILABILITY_STATUS_PREFIX$UNAVAILABLE", result.label)
	}
}
