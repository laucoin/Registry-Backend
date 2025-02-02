package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.VehicleReaderDto
import java.util.Locale
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class VehicleReaderDtoMapper(private val eventMapper: EventReaderDtoMapper):
    IGenericReaderDtoMapper<VehicleModel, VehicleReaderDto> {
    override fun toDto(model: VehicleModel, locale: Locale): VehicleReaderDto {
        return VehicleReaderDto(
            id = model.id,
            event = if (Objects.nonNull(model.event)) eventMapper.toDto(model.event !!, locale) else null,
            registration = model.registration,
            brand = model.brand,
            model = model.model,
            begin = model.begin,
            end = model.end,
            visible = model.visible,
            creation = model.creation,
            lastEdition = model.lastEdition,
        )
    }
}
