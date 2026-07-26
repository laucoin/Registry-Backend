package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReaderDto.MovementContentReaderDto
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.assertEquals

class MovementContentsReaderDtoMapperTest {
	private val contentMapper: MovementContentReaderDtoMapper = mock()
	private val mapper: MovementContentsReaderDtoMapper = MovementContentsReaderDtoMapper(contentMapper)

	@Test
	fun `Should toDto convert a movement contents Pair to MovementContentsReaderDto`() {
		// Arrange
		val movementId = UUID.randomUUID()
		val contents = listOf(MovementContentModel(), MovementContentModel())
		val contentDto = MovementContentReaderDto()

		whenever(contentMapper.toDto(any())).thenReturn(contentDto)

		// Act
		val result = mapper.toDto(Pair(movementId, contents))

		// Assert
		assertEquals(movementId, result.movementId)
		assertEquals(listOf(contentDto, contentDto), result.contents)
		verify(contentMapper, times(contents.size)).toDto(any())
	}
}
