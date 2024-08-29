package fr.laucoin.registry.backend.infrastructure.internal.web.mapper

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.CurrentUserDto
import org.springframework.stereotype.Component

@Component
class CurrentUserDtoMapper {
    fun toDto(model: CurrentUserModel): CurrentUserDto {
        return CurrentUserDto(
            id = model.id,
            authorities = model.authorities.map { it.authority },
            preferences = model.preferences,
            oidcId = model.oidcId,
            type = model.type,
            firstName = model.firstName,
            lastName = model.lastName,
            email = model.email,
            role = model.role,
            birthday = model.birthday,
            lastLogin = model.lastLogin,
            purged = model.purged,
            visible = model.visible,
            creation = model.creation,
            lastEdition = model.lastEdition,
        )
    }
}
