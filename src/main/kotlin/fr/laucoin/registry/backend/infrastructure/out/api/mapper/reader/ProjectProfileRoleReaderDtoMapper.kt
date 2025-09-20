package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_PROFILE_ROLE_PREFIX
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import java.util.Locale
import org.springframework.stereotype.Component

@Component
class ProjectProfileRoleReaderDtoMapper(
	private val translateService: ITranslateService,
): IGenericReaderDtoMapper<String, LabelDto> {
	override fun toDto(model: String, locale: Locale): LabelDto {
		return LabelDto(
			model,
			translateService.getMessage(code = "$PROJECT_PROFILE_ROLE_PREFIX$model", locale = locale),
		)
	}
}
