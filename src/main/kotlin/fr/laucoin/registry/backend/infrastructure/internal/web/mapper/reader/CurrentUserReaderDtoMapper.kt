package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.USER_ROLE_PREFIX
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.CurrentUserReaderDto
import java.util.Locale
import java.util.Objects
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class CurrentUserReaderDtoMapper(
    @Qualifier("messagesSource") private val translateService: MessageSource,
): IGenericReaderDtoMapper<CurrentUserModel, CurrentUserReaderDto> {
    override fun toDto(model: CurrentUserModel, locale: Locale): CurrentUserReaderDto {
        return CurrentUserReaderDto(
            id = model.id,
            authorities = model.authorities.map { it.authority },
            preferences = model.preferences,
            firstName = model.firstName,
            lastName = model.lastName,
            email = model.email,
            role = if (Objects.nonNull(model.role)) LabelDto(
                model.role !!,
                translateService.getMessage("$USER_ROLE_PREFIX${model.role}", null, locale),
            ) else null,
            birthday = model.birthday,
            lastLogin = model.lastLogin,
            purged = model.purged,
            visible = model.visible,
            creation = model.creation,
            lastEdition = model.lastEdition,
        )
    }
}
