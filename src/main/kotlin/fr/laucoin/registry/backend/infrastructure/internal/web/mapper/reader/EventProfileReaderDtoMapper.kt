package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.EVENT_PROFILE_ROLE_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.EVENT_PROFILE_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.BLOCKED
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.EventProfileReaderDto
import java.util.Locale
import java.util.Objects
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class EventProfileReaderDtoMapper(
    @Qualifier("messagesSource") private val translateService: MessageSource,
    private val eventMapper: EventReaderDtoMapper,
    private val partialUserMapper: PartialUserReaderDtoMapper,
): IGenericReaderDtoMapper<EventProfileModel, EventProfileReaderDto> {
    override fun toDto(model: EventProfileModel, locale: Locale): EventProfileReaderDto {
        return EventProfileReaderDto(
            user = if (Objects.nonNull(model.user)) partialUserMapper.toDto(model.user !!, locale) else null,
            role = if (Objects.nonNull(model.role)) LabelDto(
                model.role !!,
                translateService.getMessage("$EVENT_PROFILE_ROLE_PREFIX${model.role}", null, locale),
            ) else null,
            status = buildStatus(model, locale),
            startAccess = model.startAccess,
            endAccess = model.endAccess,
        ).apply {
            id = model.id
            event = if (Objects.nonNull(model.event)) eventMapper.toDto(model.event !!, locale) else null
            visible = model.visible
            creation = model.creation
            lastEdition = model.lastEdition
        }
    }

    private fun buildStatus(model: EventProfileModel, locale: Locale): LabelDto? {
        val originalStatus = translateService.getMessage("$EVENT_PROFILE_STATUS_PREFIX${model.status}", null, locale)
        if (! model.visible) {
            return LabelDto(
                BLOCKED.name,
                if (Objects.nonNull(model.status)) translateService.getMessage(
                    "$EVENT_PROFILE_STATUS_PREFIX${BLOCKED}_WITH_STATUS",
                    arrayOf(originalStatus),
                    locale
                )
                else translateService.getMessage("$EVENT_PROFILE_STATUS_PREFIX$BLOCKED", null, locale),
            )
        }

        return if (Objects.nonNull(model.status)) LabelDto(
            model.status !!.name,
            originalStatus,
        ) else null
    }
}
