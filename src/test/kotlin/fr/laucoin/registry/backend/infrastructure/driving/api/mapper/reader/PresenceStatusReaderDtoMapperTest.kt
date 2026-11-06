package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.OVERDUE
import fr.laucoin.registry.backend.domain.service.support.ITranslateService
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.LabelMappingHelper
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class PresenceStatusReaderDtoMapperTest {
	private val translateService: ITranslateService = mock()
	private val mapper = PresenceStatusReaderDtoMapperImpl(LabelMappingHelper(translateService))

	@Test
	fun `Should toDto label a presence status with its generic translation`() {
		// Arrange
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull(), any())).thenAnswer { it.arguments[0] }

		// Act
		val result = mapper.toDto(OVERDUE)

		// Assert
		assertEquals("OVERDUE", result.value)
		assertEquals("presence.status.OVERDUE", result.label)
	}
}
