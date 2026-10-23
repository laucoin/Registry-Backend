package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ALERT_CRITICALITY_RULE_KEY
import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AlertCriticalityMetadataReaderDtoMapperTest {
	private val criticalityMapper: AlertCriticalityReaderDtoMapper = mock()
	private val translateService: ITranslateService = mock()
	private val mapper = AlertCriticalityMetadataReaderDtoMapper(criticalityMapper, translateService, 6, 18)

	@Test
	fun `Should toDto map every level and describe the rule with the configured thresholds`() {
		// Arrange
		whenever(criticalityMapper.toDto(any())).thenAnswer { LabelDto(it.getArgument<AlertCriticalityEnum>(0).name, "label") }
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull(), any())).thenAnswer {
			"${it.getArgument<String>(0)}:${it.getArgument<Array<Any>>(1).joinToString(",")}"
		}

		// Act
		val result = mapper.toDto(AlertCriticalityEnum.entries)

		// Assert
		assertEquals(AlertCriticalityEnum.entries.map { it.name }, result.levels.map { it.value })
		val levels = AlertCriticalityEnum.entries.joinToString(" \u00B7 ") { "alert.criticality.rule.$it:6,18" }
		assertEquals("$ALERT_CRITICALITY_RULE_KEY:$levels", result.rule)
		verify(translateService).getMessage(eq(ALERT_CRITICALITY_RULE_KEY), eq(arrayOf<Any>(levels)), anyOrNull(), any())
	}
}
