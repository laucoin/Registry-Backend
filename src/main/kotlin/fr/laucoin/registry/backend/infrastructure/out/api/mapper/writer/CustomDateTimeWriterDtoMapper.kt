package fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.CustomDateTimeWriterDto
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class CustomDateTimeWriterDtoMapper : IGenericWriterDtoMapper<CustomDateTimeModel?, CustomDateTimeWriterDto> {
	override fun toModel(dto: CustomDateTimeWriterDto): CustomDateTimeModel? {
		return Optional.ofNullable(dto.date).map { CustomDateTimeModel(it, dto.time) }.orElse(null)
	}
}
