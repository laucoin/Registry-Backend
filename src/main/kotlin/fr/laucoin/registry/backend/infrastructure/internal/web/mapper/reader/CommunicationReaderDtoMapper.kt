package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.CommunicationReaderDto
import java.util.Locale
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class CommunicationReaderDtoMapper(
    private val projectMapper: ProjectReaderDtoMapper,
    private val movementMapper: MovementReaderDtoMapper,
):
    IGenericReaderDtoMapper<CommunicationModel, CommunicationReaderDto> {
    override fun toDto(model: CommunicationModel, locale: Locale): CommunicationReaderDto {
        return CommunicationReaderDto(
            dateTime = model.dateTime,
            message = model.message,
            movement = if (Objects.nonNull(model.movement)) movementMapper.toDto(model.movement !!, locale) else null,
        ).apply {
            id = model.id
            project = if (Objects.nonNull(model.project)) projectMapper.toDto(model.project !!, locale) else null
            visible = model.visible
            creation = model.creation
            lastEdition = model.lastEdition
        }
    }
}
