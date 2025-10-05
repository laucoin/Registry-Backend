package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_PROFILE_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.service.ITranslateService
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ProjectProfileStatusReaderDtoMapperTest {
	private val translateService: ITranslateService = mock()
	private val mapper: ProjectProfileStatusReaderDtoMapper = ProjectProfileStatusReaderDtoMapper(translateService)

	@Test
	fun `Should toDto convert status as String to LabelDto`() {
		// Arrange
		val status = ACCEPTED
		val translated = "translated"
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull())).thenReturn(translated)

		// Act
		val result = mapper.toDto(status)

		// Assert
		verify(translateService).getMessage("${PROJECT_PROFILE_STATUS_PREFIX}$status")

		assertEquals(status.name, result.value)
		assertEquals(translated, result.label)
	}
}
