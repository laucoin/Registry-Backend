package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.VehicleReaderDto
import java.util.Locale
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class VehicleReaderDtoMapper(
    private val projectMapper: ProjectReaderDtoMapper,
    private val statusMapper: PresenceStatusReaderDtoMapper,
): IGenericReaderDtoMapper<VehicleModel, VehicleReaderDto> {
    override fun toDto(model: VehicleModel, locale: Locale): VehicleReaderDto {
        return VehicleReaderDto(
            licensePlate = model.licensePlate,
            brand = model.brand,
            model = model.model,
            status = if (Objects.nonNull(model.status)) statusMapper.toDto(
                model.status !!,
                locale,
                model.lastMovement,
                model.startAvailability,
                model.endAvailability,
            ) else null,
            startAvailability = model.startAvailability,
            endAvailability = model.endAvailability,
        ).apply {
            id = model.id
            project = if (Objects.nonNull(model.project)) projectMapper.toDto(model.project !!, locale) else null
            visible = model.visible
            creation = model.creation
            lastEdition = model.lastEdition
        }
    }
}
