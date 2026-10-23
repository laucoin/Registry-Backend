package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonKindEnum.REASON
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto.MovementContentReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReasonsReaderDto
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class PlannedMovementReaderDtoMapperTest {
	private val movementMapper: MovementReaderDtoMapper = mock()
	private val mapper = PlannedMovementReaderDtoMapper(movementMapper)

	@Test
	fun `Should toDto keep the movement shape without date-time`() {
		// Arrange
		val model = MovementModel(contentType = REGISTERED)
		val content = listOf(MovementContentReaderDto(poolName = "pool"))
		whenever(movementMapper.toDto(model)).thenReturn(
			MovementReaderDto(type = LabelDto("IN", "In"), contentType = REGISTERED, content = content)
		)

		// Act
		val dto = mapper.toDto(model)

		// Assert
		assertEquals(LabelDto("IN", "In"), dto.type)
		assertNull(dto.reason)
		assertEquals(REGISTERED, dto.contentType)
		assertEquals(content, dto.content)
	}

	@Test
	fun `Should toDto keep the reason and an empty content`() {
		// Arrange
		val model = MovementModel(contentType = REGISTERED)
		val reason = MovementReasonsReaderDto(label = "Definitive departure", value = "DEFINITIVE_DEPARTURE", type = OUT, kind = REASON)
		whenever(movementMapper.toDto(model)).thenReturn(MovementReaderDto(reason = reason, contentType = REGISTERED))

		// Act
		val dto = mapper.toDto(model)

		// Assert
		assertEquals(reason, dto.reason)
		assertEquals(emptyList(), dto.content)
	}
}
