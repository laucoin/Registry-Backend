package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.CreatedProjectProfilesReaderDto
import java.util.Locale
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class CreatedProjectProfilesReaderDtoMapper: IGenericReaderDtoMapper<Pair<List<UUID>, List<UUID>>, CreatedProjectProfilesReaderDto> {
    override fun toDto(model: Pair<List<UUID>, List<UUID>>, locale: Locale): CreatedProjectProfilesReaderDto {
        return CreatedProjectProfilesReaderDto(
            createdUserIds = model.first,
            notCreatedUserIds = model.second,
        )
    }
}
