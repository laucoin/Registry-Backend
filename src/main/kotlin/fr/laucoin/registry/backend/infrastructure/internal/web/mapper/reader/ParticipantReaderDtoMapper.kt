package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ParticipantReaderDto
import java.time.LocalDate
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class ParticipantReaderDtoMapper(
    private val partialUserMapper: PartialUserReaderDtoMapper,
    private val groupMapper: GroupReaderDtoMapper
): IGenericReaderDtoMapper<ParticipantModel, ParticipantReaderDto> {
    override fun toDto(model: ParticipantModel): ParticipantReaderDto {
        return ParticipantReaderDto(
            id = model.id,
            event = model.event,
            firstName = model.firstName,
            lastName = model.lastName,
            birthday = model.birthday,
            major = isMajor(model.birthday),
            groups = groupMapper.toDtoList(model.groups),
            begin = model.begin,
            end = model.end,
            user = if (Objects.nonNull(model.user)) partialUserMapper.toDto(model.user !!) else null,
            purged = model.purged,
            visible = model.visible,
            creation = model.creation,
            lastEdition = model.lastEdition,
        )
    }

    private fun isMajor(birthday: LocalDate?): Boolean {
        val now = LocalDate.now()
        val minBirthday = now.minusYears(18)
        return birthday?.let {
            minBirthday.isAfter(it)
            || minBirthday.isEqual(it)
        } ?: false
    }
}
