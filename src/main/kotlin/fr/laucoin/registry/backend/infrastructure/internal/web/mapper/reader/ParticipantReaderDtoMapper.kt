package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.USABLE_ELEMENT_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ParticipantReaderDto
import java.time.LocalDate
import java.util.Locale
import java.util.Objects
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class ParticipantReaderDtoMapper(
    @Qualifier("messagesSource") private val translateService: MessageSource,
    private val partialUserMapper: PartialUserReaderDtoMapper,
    private val eventMapper: EventReaderDtoMapper,
    private val groupMapper: GroupWithoutMemberReaderDtoMapper
): IGenericReaderDtoMapper<ParticipantModel, ParticipantReaderDto> {
    override fun toDto(model: ParticipantModel, locale: Locale): ParticipantReaderDto {
        return ParticipantReaderDto(
            firstName = model.firstName,
            lastName = model.lastName,
            birthday = model.birthday,
            major = isMajor(model.birthday),
            groups = groupMapper.toDtoList(model.groups, locale),
            availableGroups = groupMapper.toDtoList(model.availableGroups, locale),
            status = if (Objects.nonNull(model.status)) LabelDto(
                model.status !!.name,
                translateService.getMessage("$USABLE_ELEMENT_STATUS_PREFIX${model.status}", null, locale),
            ) else null,
            startAvailability = model.startAvailability,
            endAvailability = model.endAvailability,
            user = if (Objects.nonNull(model.user)) partialUserMapper.toDto(model.user !!, locale) else null,
            purged = model.purged,
        ).apply {
            id = model.id
            event = if (Objects.nonNull(model.event)) eventMapper.toDto(model.event !!, locale) else null
            visible = model.visible
            creation = model.creation
            lastEdition = model.lastEdition
        }
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
