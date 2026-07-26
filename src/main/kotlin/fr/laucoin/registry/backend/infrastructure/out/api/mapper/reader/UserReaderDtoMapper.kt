package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.USER_ROLE_PREFIX
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.UserReaderDto
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class UserReaderDtoMapper(
	private val translateService: ITranslateService,
) : IGenericReaderDtoMapper<UserModel, UserReaderDto> {
	override fun toDto(model: UserModel): UserReaderDto {
		return UserReaderDto(
			firstName = model.firstName,
			lastName = model.lastName,
			email = model.email,
			role = Optional.ofNullable(model.role).map {
				LabelDto(
					it,
					translateService.getMessage(code = "$USER_ROLE_PREFIX$it"),
				)
			}.orElse(null),
			birthday = model.birthday,
			lastLogin = model.lastLogin,
		).apply {
			id = model.id
			visible = model.visible
			creation = model.creation
			lastEdition = model.lastEdition
		}
	}
}
