package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_PROFILE_ROLE_PREFIX
import fr.laucoin.registry.backend.domain.service.ITranslateService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

class ProjectProfileRoleReaderDtoMapperTest {
	private val translateService: ITranslateService = mock()
	private val mapper: ProjectProfileRoleReaderDtoMapper = ProjectProfileRoleReaderDtoMapper(translateService)

	@Test
	fun `Should toDto convert role as String to LabelDto`() {
		// Arrange
		val role = "ROLE"
		val translated = "translated"
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull())).thenReturn(translated)

		// Act
		val result = mapper.toDto(role)

		// Assert
		verify(translateService).getMessage("${PROJECT_PROFILE_ROLE_PREFIX}ROLE")

		assertEquals(role, result.value)
		assertEquals(translated, result.label)
	}
}
