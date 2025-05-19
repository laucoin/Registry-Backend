package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.USER_ROLE_PREFIX
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.CurrentUserReaderDto
import java.util.Locale
import java.util.Optional
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class CurrentUserReaderDtoMapper(
    @Qualifier("messagesSource") private val translateService: MessageSource,
    private val preferenceMapper: PreferenceReaderDtoMapper,
): IGenericReaderDtoMapper<CurrentUserModel, CurrentUserReaderDto> {
    override fun toDto(model: CurrentUserModel, locale: Locale): CurrentUserReaderDto {
        return CurrentUserReaderDto(
            authorities = model.authorities.map { it.authority },
            preferences = Optional.ofNullable(model.preferences).map { preferenceMapper.toDto(it, locale) }
                .orElse(null),
            firstName = model.firstName,
            lastName = model.lastName,
            email = model.email,
            role = Optional.ofNullable(model.role).map {
                LabelDto(
                    it,
                    translateService.getMessage("$USER_ROLE_PREFIX$it", null, locale),
                )
            }.orElse(null),
            birthday = model.birthday,
            lastLogin = model.lastLogin,
            purged = model.purged,
        ).apply {
            id = model.id
            visible = model.visible
            creation = model.creation
            lastEdition = model.lastEdition
        }
    }
}
