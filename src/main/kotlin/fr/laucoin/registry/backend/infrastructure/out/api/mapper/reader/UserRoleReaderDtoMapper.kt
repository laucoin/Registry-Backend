package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.USER_ROLE_PREFIX
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import org.springframework.stereotype.Component

@Component
class UserRoleReaderDtoMapper(
	private val translateService: ITranslateService,
) : IGenericReaderDtoMapper<String, LabelDto> {
	override fun toDto(model: String): LabelDto {
		return LabelDto(
			model,
			translateService.getMessage(code = "$USER_ROLE_PREFIX$model"),
		)
	}
}
