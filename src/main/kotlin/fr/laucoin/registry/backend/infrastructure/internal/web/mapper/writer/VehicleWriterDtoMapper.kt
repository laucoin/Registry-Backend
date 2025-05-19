package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.VehicleWriterDto
import java.util.Optional
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class VehicleWriterDtoMapper(
    private val customDateTimeMapper: CustomDateTimeWriterDtoMapper,
): IGenericProjectWriterDtoMapper<VehicleModel, VehicleWriterDto> {
    override fun toModel(dto: VehicleWriterDto, projectId: UUID): VehicleModel {
        return VehicleModel().apply {
            licensePlate = dto.licensePlate?.uppercase()
            brand = dto.brand
            model = dto.model
            startAvailability =
                Optional.ofNullable(dto.startAvailability).map { customDateTimeMapper.toModel(it) }.orElse(null)
            endAvailability = Optional.ofNullable(dto.endAvailability).map { customDateTimeMapper.toModel(it) }.orElse(null)
            project = ProjectModel().apply { id = projectId }
        }
    }
}
