package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.AVAILABILITY_STATUS_DURATION_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.AVAILABILITY_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum.AVAILABLE
import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum.UNAVAILABLE
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AvailabilityStatusReaderDtoMapperTest {
	private val translateService: ITranslateService = mock()
	private val mapper = AvailabilityStatusReaderDtoMapper(translateService)

	private fun daysFromNow(days: Long) = CustomDateTimeModel(LocalDate.now().plusDays(days))

	@Test
	fun `Should fall back to the plain status label when the project has no dates`() {
		// Arrange
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull())).thenReturn("translated")

		// Act
		val result = mapper.toDto(AVAILABLE, null, null)

		// Assert
		assertEquals(AVAILABLE.name, result.value)
		assertEquals("translated", result.label)
		verify(translateService).getMessage(code = "$AVAILABILITY_STATUS_PREFIX$AVAILABLE")
	}

	@Test
	fun `Should describe how long the project has been available when only the start is set`() {
		// Arrange
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull())).thenReturn("translated")

		// Act
		val result = mapper.toDto(AVAILABLE, daysFromNow(-2), null)

		// Assert
		assertEquals(AVAILABLE.name, result.value)
		verify(translateService).getMessage(eq("$AVAILABILITY_STATUS_DURATION_PREFIX$AVAILABLE"), anyOrNull(), anyOrNull())
	}

	@Test
	fun `Should use the NOT_YET duration message when a start date is set`() {
		// Arrange
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull())).thenReturn("translated")

		// Act
		mapper.toDto(UNAVAILABLE, daysFromNow(-2), null)

		// Assert
		verify(translateService).getMessage(eq("${AVAILABILITY_STATUS_DURATION_PREFIX}NOT_YET"), anyOrNull(), anyOrNull())
	}

	@Test
	fun `Should use the NO_MORE duration message when only an end date is set`() {
		// Arrange
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull())).thenReturn("translated")

		// Act
		mapper.toDto(UNAVAILABLE, null, daysFromNow(5))

		// Assert
		verify(translateService).getMessage(eq("${AVAILABILITY_STATUS_DURATION_PREFIX}NO_MORE"), anyOrNull(), anyOrNull())
	}
}
