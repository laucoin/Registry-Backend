package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum.AVAILABLE
import fr.laucoin.registry.backend.domain.service.support.ITranslateService
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.LabelMappingHelper
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AvailabilityStatusReaderDtoMapperTest {
	private val translateService: ITranslateService = mock()
	private val mapper = AvailabilityStatusReaderDtoMapperImpl(LabelMappingHelper(translateService))

	@Test
	fun `Should keep the generic translations for the metadata labels`() {
		// Arrange
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull(), any()))
			.thenAnswer { "${it.getArgument<String>(0)}|${it.getArgument<Array<Any>?>(1)?.joinToString()}" }

		// Act
		val label = mapper.toDto(AVAILABLE)

		// Assert
		assertEquals("AVAILABLE", label.value)
		assertEquals("availability.status.AVAILABLE|null", label.label)
	}
}
