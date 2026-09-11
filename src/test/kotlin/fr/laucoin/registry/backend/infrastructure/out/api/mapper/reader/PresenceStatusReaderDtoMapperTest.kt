package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PRESENCE_STATUS_DURATION_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PRESENCE_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.UNAVAILABLE
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PresenceStatusReaderDtoMapperTest {
	private val translateService: ITranslateService = mock()
	private val mapper = PresenceStatusReaderDtoMapper(translateService)

	private fun daysFromNow(days: Long) = CustomDateTimeModel(LocalDate.now().plusDays(days))

	@Test
	fun `Should fall back to NOT_ARRIVED_YET when there is no movement and no availability dates`() {
		// Arrange
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull())).thenReturn("translated")

		// Act
		val result = mapper.toDto(UNAVAILABLE, null, null, null)

		// Assert
		assertEquals(UNAVAILABLE.name, result.value)
		assertEquals("translated", result.label)
		verify(translateService).getMessage(code = "${PRESENCE_STATUS_PREFIX}NOT_ARRIVED_YET")
	}

	@Test
	fun `Should describe the elapsed time since the last movement`() {
		// Arrange
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull())).thenReturn("translated")

		// Act
		val result = mapper.toDto(IN, ZonedDateTime.now().minusHours(2), null, null)

		// Assert
		assertEquals(IN.name, result.value)
		verify(translateService).getMessage(eq("$PRESENCE_STATUS_DURATION_PREFIX$IN"), anyOrNull(), anyOrNull())
	}

	@Test
	fun `Should use the ARRIVE duration message when a start date is set`() {
		// Arrange
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull())).thenReturn("translated")

		// Act
		mapper.toDto(UNAVAILABLE, null, daysFromNow(-2), null)

		// Assert
		verify(translateService).getMessage(eq("${PRESENCE_STATUS_DURATION_PREFIX}ARRIVE"), anyOrNull(), anyOrNull())
	}

	@Test
	fun `Should use the LEFT duration message when only an end date is set`() {
		// Arrange
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull())).thenReturn("translated")

		// Act
		mapper.toDto(UNAVAILABLE, null, null, daysFromNow(5))

		// Assert
		verify(translateService).getMessage(eq("${PRESENCE_STATUS_DURATION_PREFIX}LEFT"), anyOrNull(), anyOrNull())
	}
}
