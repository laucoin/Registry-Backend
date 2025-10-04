package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CreatedProjectProfilesReaderDto
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class CreatedProjectProfilesReaderDtoMapper:
	IGenericReaderDtoMapper<Pair<List<UUID>, List<UUID>>, CreatedProjectProfilesReaderDto> {
	override fun toDto(model: Pair<List<UUID>, List<UUID>>): CreatedProjectProfilesReaderDto {
		return CreatedProjectProfilesReaderDto(
			createdUserIds = model.first,
			notCreatedUserIds = model.second,
		)
	}
}
