package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.USER_ROLE_PREFIX
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import java.util.Locale
import org.springframework.stereotype.Component

@Component
class UserRoleReaderDtoMapper(
    private val translateService: ITranslateService,
) : IGenericReaderDtoMapper<String, LabelDto> {
    override fun toDto(model: String, locale: Locale): LabelDto {
        return LabelDto(
            model,
            translateService.getMessage(code = "$USER_ROLE_PREFIX$model", locale = locale),
        )
    }
}
