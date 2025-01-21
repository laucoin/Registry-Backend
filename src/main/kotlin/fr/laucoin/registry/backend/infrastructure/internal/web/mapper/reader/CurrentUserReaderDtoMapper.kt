package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.CurrentUserReaderDto
import org.springframework.stereotype.Component

@Component
class CurrentUserReaderDtoMapper: IGenericReaderDtoMapper<CurrentUserModel, CurrentUserReaderDto> {
    override fun toDto(model: CurrentUserModel): CurrentUserReaderDto {
        return CurrentUserReaderDto(
            id = model.id,
            authorities = model.authorities.map { it.authority },
            preferences = model.preferences,
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
