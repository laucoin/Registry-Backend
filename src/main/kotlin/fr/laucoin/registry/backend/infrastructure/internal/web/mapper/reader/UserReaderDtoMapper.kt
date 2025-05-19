package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.USER_ROLE_PREFIX
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.UserReaderDto
import java.util.Locale
import java.util.Optional
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class UserReaderDtoMapper(
    @Qualifier("messagesSource") private val translateService: MessageSource,
): IGenericReaderDtoMapper<UserModel, UserReaderDto> {
    override fun toDto(model: UserModel, locale: Locale): UserReaderDto {
        return UserReaderDto(
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
