package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.PartialUserReaderDto
import org.springframework.stereotype.Component

@Component
class PartialUserReaderDtoMapper : IGenericReaderDtoMapper<UserModel, PartialUserReaderDto> {
	override fun toDto(model: UserModel): PartialUserReaderDto {
		return PartialUserReaderDto(
			id = model.id,
			firstName = model.firstName,
			lastName = model.lastName,
			email = model.email
		)
	}
}
