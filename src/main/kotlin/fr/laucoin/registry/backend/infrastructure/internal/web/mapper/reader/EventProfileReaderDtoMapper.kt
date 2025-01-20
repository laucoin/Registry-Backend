package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.EventProfileReaderDto
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class EventProfileReaderDtoMapper(
    private val partialUserMapper: PartialUserReaderDtoMapper
): IGenericReaderDtoMapper<EventProfileModel, EventProfileReaderDto> {
    override fun toDto(model: EventProfileModel): EventProfileReaderDto {
        return EventProfileReaderDto(
            id = model.id,
            event = model.event,
            user = if (Objects.nonNull(model.user)) partialUserMapper.toDto(model.user !!) else null,
            role = model.role,
            status = model.status,
            startAccess = model.startAccess,
            endAccess = model.endAccess,
            visible = model.visible,
            creation = model.creation,
            lastEdition = model.lastEdition,
        )
    }
}
