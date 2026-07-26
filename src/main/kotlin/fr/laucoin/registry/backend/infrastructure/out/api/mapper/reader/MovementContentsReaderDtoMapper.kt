package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementContentsReaderDto
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MovementContentsReaderDtoMapper(
	private val contentMapper: MovementContentReaderDtoMapper,
) : IGenericReaderDtoMapper<Pair<UUID, List<MovementContentModel>>, MovementContentsReaderDto> {
	override fun toDto(model: Pair<UUID, List<MovementContentModel>>): MovementContentsReaderDto {
		return MovementContentsReaderDto(
			movementId = model.first,
			contents = model.second.map(contentMapper::toDto),
		)
	}
}
