package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CreatedProjectProfilesReaderDto
import org.springframework.stereotype.Component

@Component
class CreatedProjectProfilesReaderDtoMapper:
	IGenericReaderDtoMapper<Pair<List<String>, List<String>>, CreatedProjectProfilesReaderDto> {
	override fun toDto(model: Pair<List<String>, List<String>>): CreatedProjectProfilesReaderDto {
		return CreatedProjectProfilesReaderDto(
			createdEmails = model.first,
			notCreatedEmails = model.second,
		)
	}
}
