package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementParticipantsAndGroupsReaderDto
import java.util.Locale
import org.springframework.stereotype.Component

@Component
class MovementParticipantsAndGroupsReaderDtoMapper(
    private val participantReaderMapper: ParticipantReaderDtoMapper,
    private val groupReaderMapper: GroupReaderDtoMapper,
): IGenericReaderDtoMapper<Pair<List<ParticipantModel>, List<GroupModel>>, MovementParticipantsAndGroupsReaderDto> {
    override fun toDto(
        model: Pair<List<ParticipantModel>, List<GroupModel>>,
        locale: Locale
    ): MovementParticipantsAndGroupsReaderDto {
        return MovementParticipantsAndGroupsReaderDto(
            participants = participantReaderMapper.toDtoList(model.first, locale),
            groups = groupReaderMapper.toDtoList(model.second, locale),
        )
    }
}
