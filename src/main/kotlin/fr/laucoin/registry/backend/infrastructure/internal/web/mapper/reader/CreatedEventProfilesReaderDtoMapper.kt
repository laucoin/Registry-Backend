package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.CreatedEventProfilesReaderDto
import java.util.Locale
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class CreatedEventProfilesReaderDtoMapper: IGenericReaderDtoMapper<Pair<List<UUID>, List<UUID>>, CreatedEventProfilesReaderDto> {
    override fun toDto(model: Pair<List<UUID>, List<UUID>>, locale: Locale): CreatedEventProfilesReaderDto {
        return CreatedEventProfilesReaderDto(
            createdUserIds = model.first,
            notCreatedUserIds = model.second,
        )
    }
}
