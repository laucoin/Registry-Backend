package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ALERT_CRITICALITY_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum.REINFORCED
import fr.laucoin.registry.backend.domain.service.ITranslateService
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AlertCriticalityReaderDtoMapperTest {
	private val translateService: ITranslateService = mock()
	private val mapper: AlertCriticalityReaderDtoMapper = AlertCriticalityReaderDtoMapper(translateService)

	@Test
	fun `Should toDto convert criticality to LabelDto`() {
		// Arrange
		val criticality = REINFORCED
		val translated = "translated"
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull(), any())).thenReturn(translated)

		// Act
		val result = mapper.toDto(criticality)

		// Assert
		verify(translateService).getMessage("$ALERT_CRITICALITY_PREFIX$criticality")

		assertEquals(criticality.name, result.value)
		assertEquals(translated, result.label)
	}
}
