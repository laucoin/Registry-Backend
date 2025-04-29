package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ActivityReaderDto
import java.util.Locale
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class ActivityReaderDtoMapper(private val projectMapper: ProjectReaderDtoMapper):
    IGenericReaderDtoMapper<ActivityModel, ActivityReaderDto> {
    override fun toDto(model: ActivityModel, locale: Locale): ActivityReaderDto {
        return ActivityReaderDto(
            name = model.name,
            description = model.description,
            duration = if (Objects.nonNull(model.duration)) LabelDto(
                label = model.duration !!.toString(),
                value = model.duration !!.toIsoString()
            ) else null,
            allowedParticipants = model.allowedParticipants,
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
