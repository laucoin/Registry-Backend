package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.USER_ROLE_PREFIX
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.CurrentUserReaderDto
import java.util.Locale
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class CurrentUserReaderDtoMapper(
    private val translateService: ITranslateService,
    private val preferenceMapper: PreferenceReaderDtoMapper,
) : IGenericReaderDtoMapper<CurrentUserModel, CurrentUserReaderDto> {
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
                    translateService.getMessage(code = "$USER_ROLE_PREFIX$it", locale = locale),
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
