package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_OPTION_FORM_ASK_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_OPTION_NAME_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ProjectOptionsReaderDto
import java.util.Locale
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class ProjectOptionsReaderDtoMapper(
    @Qualifier("messagesSource") private val translateService: MessageSource,
): IGenericReaderDtoMapper<ProjectOptionEnum, ProjectOptionsReaderDto> {
    override fun toDto(model: ProjectOptionEnum, locale: Locale): ProjectOptionsReaderDto {
        return ProjectOptionsReaderDto(
            value = model,
            label = translateService.getMessage("$PROJECT_OPTION_NAME_PREFIX$model", null, locale),
            ask = translateService.getMessage("$PROJECT_OPTION_FORM_ASK_PREFIX$model", null, locale),
            preRequired = model.requiredOptions.map {
                LabelDto(
                    it.name,
                    translateService.getMessage("$PROJECT_OPTION_NAME_PREFIX$it", null, locale)
                )
            }
        )
    }
}
