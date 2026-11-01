package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.FeaturesReaderDto
import org.springframework.stereotype.Component

@Component
class FeaturesReaderDtoMapper: IGenericReaderDtoMapper<Boolean, FeaturesReaderDto> {
	override fun toDto(model: Boolean): FeaturesReaderDto {
		return FeaturesReaderDto(inviteByEmail = model)
	}
}
