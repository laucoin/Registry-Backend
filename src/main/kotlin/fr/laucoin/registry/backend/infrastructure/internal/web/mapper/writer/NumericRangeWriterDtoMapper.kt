package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.model.NumericRangeModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.NumericRangeWriterDto
import org.springframework.stereotype.Component

@Component
class NumericRangeWriterDtoMapper: IGenericWriterDtoMapper<NumericRangeModel, NumericRangeWriterDto> {
    override fun toModel(dto: NumericRangeWriterDto): NumericRangeModel {
        return NumericRangeModel().apply {
            lower = dto.lower
            upper = dto.upper
        }
    }
}
