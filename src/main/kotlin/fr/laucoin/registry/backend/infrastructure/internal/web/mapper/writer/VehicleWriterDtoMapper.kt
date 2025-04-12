package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.VehicleWriterDto
import java.util.Objects
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class VehicleWriterDtoMapper(
    private val customDateTimeMapper: CustomDateTimeWriterDtoMapper,
): IGenericEventWriterDtoMapper<VehicleModel, VehicleWriterDto> {
    override fun toModel(dto: VehicleWriterDto, eventId: UUID): VehicleModel {
        return VehicleModel().apply {
            licensePlate = dto.licensePlate?.uppercase()
            brand = dto.brand
            model = dto.model
            startAvailability =
                if (Objects.nonNull(dto.startAvailability)) customDateTimeMapper.toModel(dto.startAvailability !!) else null
            endAvailability = if (Objects.nonNull(dto.endAvailability)) customDateTimeMapper.toModel(dto.endAvailability !!) else null
            event = EventModel().apply { id = eventId }
        }
    }
}
