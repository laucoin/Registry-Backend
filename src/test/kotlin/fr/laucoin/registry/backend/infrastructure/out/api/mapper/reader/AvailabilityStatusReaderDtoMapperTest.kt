package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.AVAILABILITY_STATUS_DURATION_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.AVAILABILITY_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum.AVAILABLE
import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum.UNAVAILABLE
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate
import kotlin.test.assertEquals

class AvailabilityStatusReaderDtoMapperTest {
	private val translateService: ITranslateService = mock()
	private val mapper = AvailabilityStatusReaderDtoMapper(translateService)

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
	 * The two duration branches below encode the mapper's CURRENT (frozen v1)
	 * direction semantics: NOT_YET fires when now is after the start bound and
	 * NO_MORE when now is before the end bound. The null-safety fix must not
	 * change them; revisit the direction question with the v2 contract work.
	 */
	@Test
	fun `Should announce the not-yet label when now is after the start bound`() {
		// Arrange
		val start = CustomDateTimeModel(LocalDate.now().minusDays(3))

		// Act
		val result = mapper.toDto(UNAVAILABLE, startAvailability = start, endAvailability = null)

		// Assert
		assertEquals("${AVAILABILITY_STATUS_DURATION_PREFIX}NOT_YET", result.label)
	}

	@Test
	fun `Should announce the no-more label when now is before the end bound`() {
		// Arrange
		val end = CustomDateTimeModel(LocalDate.now().plusDays(3))

		// Act
		val result = mapper.toDto(UNAVAILABLE, startAvailability = null, endAvailability = end)

		// Assert
		assertEquals("${AVAILABILITY_STATUS_DURATION_PREFIX}NO_MORE", result.label)
	}
}
