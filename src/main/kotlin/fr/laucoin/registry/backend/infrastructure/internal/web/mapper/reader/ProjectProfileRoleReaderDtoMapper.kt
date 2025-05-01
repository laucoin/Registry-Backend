package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_PROFILE_ROLE_PREFIX
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import java.util.Locale
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class ProjectProfileRoleReaderDtoMapper(
    @Qualifier("messagesSource") private val translateService: MessageSource,
): IGenericReaderDtoMapper<String, LabelDto> {
    override fun toDto(model: String, locale: Locale): LabelDto {
        return LabelDto(
            model,
            translateService.getMessage("$PROJECT_PROFILE_ROLE_PREFIX$model", null, locale),
        )
    }
}
