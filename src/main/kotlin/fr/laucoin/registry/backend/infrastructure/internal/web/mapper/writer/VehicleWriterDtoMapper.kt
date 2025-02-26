package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.VehicleWriterDto
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class VehicleWriterDtoMapper: IGenericEventWriterDtoMapper<VehicleModel, VehicleWriterDto> {
    override fun toModel(dto: VehicleWriterDto, eventId: UUID): VehicleModel {
        return VehicleModel().apply {
            registration = dto.registration?.uppercase()
            brand = dto.brand
            model = dto.model
            begin = dto.begin
            end = dto.end
            event = EventModel().apply { id = eventId }
        }
    }
}
