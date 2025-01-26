package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.PartialUserReaderDto
import java.util.Locale
import org.springframework.stereotype.Component

@Component
class PartialUserReaderDtoMapper: IGenericReaderDtoMapper<UserModel, PartialUserReaderDto> {
    override fun toDto(model: UserModel, locale: Locale): PartialUserReaderDto {
        return PartialUserReaderDto(
            id = model.id,
            firstName = model.firstName,
            lastName = model.lastName,
            email = model.email
        )
    }
}
