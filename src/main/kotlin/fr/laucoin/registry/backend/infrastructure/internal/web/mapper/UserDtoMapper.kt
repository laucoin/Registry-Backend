package fr.laucoin.registry.backend.infrastructure.internal.web.mapper

import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.UserDto
import org.springframework.stereotype.Component

@Component
class UserDtoMapper {
    fun toDto(model: UserModel): UserDto {
        return UserDto(
            id = model.id,
            firstName = model.firstName,
            lastName = model.lastName,
            email = model.email
        )
    }
}
