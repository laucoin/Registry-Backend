package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.CustomDateTimeWriterDto
import org.springframework.stereotype.Component

@Component
class CustomDateTimeWriterDtoMapper: IGenericWriterDtoMapper<CustomDateTimeModel, CustomDateTimeWriterDto> {
    override fun toModel(dto: CustomDateTimeWriterDto): CustomDateTimeModel {
        return CustomDateTimeModel(dto.date !!).apply {
            time = dto.time
        }
    }
}
