package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ParticipantReaderDto
import java.time.LocalDate
import java.util.Locale
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class ParticipantReaderDtoMapper(
    private val partialUserMapper: PartialUserReaderDtoMapper,
    private val typeMapper: ParticipantTypeReaderDtoMapper,
    private val statusMapper: PresenceStatusReaderDtoMapper,
    private val projectMapper: ProjectReaderDtoMapper,
    private val groupMapper: GroupWithoutMemberReaderDtoMapper,
): IGenericReaderDtoMapper<ParticipantModel, ParticipantReaderDto> {
    override fun toDto(model: ParticipantModel, locale: Locale): ParticipantReaderDto {
        return ParticipantReaderDto(
            firstName = model.firstName,
            lastName = model.lastName,
            birthday = model.birthday,
            type = if (Objects.nonNull(model.type)) typeMapper.toDto(model.type !!, locale) else null,
            major = isMajor(model.birthday),
            groups = groupMapper.toDtoList(model.groups, locale),
            availableGroups = groupMapper.toDtoList(model.availableGroups, locale),
            status = if (Objects.nonNull(model.status)) statusMapper.toDto(
                model.status !!,
                locale,
                model.lastMovement,
                model.startAvailability,
                model.endAvailability,
            ) else null,
            startAvailability = model.startAvailability,
            endAvailability = model.endAvailability,
            user = if (Objects.nonNull(model.user)) partialUserMapper.toDto(model.user !!, locale) else null,
            purged = model.purged,
        ).apply {
            id = model.id
            project = if (Objects.nonNull(model.project)) projectMapper.toDto(model.project !!, locale) else null
            visible = model.visible
            creation = model.creation
            lastEdition = model.lastEdition
        }
    }

    private fun isMajor(birthday: LocalDate?): Boolean {
        val today = LocalDate.now()
        val minBirthday = today.minusYears(18)
        return birthday?.let {
            minBirthday.isAfter(it)
            || minBirthday.isEqual(it)
        } ?: false
    }
}
