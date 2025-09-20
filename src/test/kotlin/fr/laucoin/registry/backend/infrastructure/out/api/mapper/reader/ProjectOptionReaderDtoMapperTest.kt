package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_OPTION_FORM_ASK_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_OPTION_NAME_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.COMMUNICATION
import fr.laucoin.registry.backend.domain.service.ITranslateService
import java.util.Locale
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ProjectOptionReaderDtoMapperTest {
	private val translateService: ITranslateService = mock()
	private val mapper: ProjectOptionsReaderDtoMapper = ProjectOptionsReaderDtoMapper(translateService)

	@Test
	fun `Should toDto convert Project option rules map to ProjectOptionModel List`() {
		// Arrange
		val option: ProjectOptionEnum = COMMUNICATION
		val label = "translated"
		whenever(translateService.getMessage(any(), any(), anyOrNull(), anyOrNull())).thenReturn(label)

		// Act
		val result = mapper.toDto(option, Locale.getDefault())

		// Assert
		verify(translateService).getMessage("${PROJECT_OPTION_NAME_PREFIX}$COMMUNICATION", Locale.getDefault())
		verify(translateService).getMessage(
			"${PROJECT_OPTION_FORM_ASK_PREFIX}$COMMUNICATION",
			Locale.getDefault()
		)
		verify(translateService).getMessage("${PROJECT_OPTION_NAME_PREFIX}$ACTIVITY", Locale.getDefault())

		assertEquals(option, result.value)
		assertEquals(label, result.label)
		assertEquals(label, result.ask)
		assertEquals(option.requiredOptions.size, result.preRequired.size)
	}
}
