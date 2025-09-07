package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_OPTION_FORM_ASK_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_OPTION_NAME_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ProjectOptionsReaderDto
import java.util.Locale
import org.springframework.stereotype.Component

@Component
class ProjectOptionsReaderDtoMapper(
    private val translateService: ITranslateService,
) : IGenericReaderDtoMapper<ProjectOptionEnum, ProjectOptionsReaderDto> {
    override fun toDto(model: ProjectOptionEnum, locale: Locale): ProjectOptionsReaderDto {
        return ProjectOptionsReaderDto(
            value = model,
            label = translateService.getMessage(code = "$PROJECT_OPTION_NAME_PREFIX$model", locale = locale),
            ask = translateService.getMessage(code = "$PROJECT_OPTION_FORM_ASK_PREFIX$model", locale = locale),
            preRequired = model.requiredOptions.map {
                LabelDto(
                    it.name,
                    translateService.getMessage(code = "$PROJECT_OPTION_NAME_PREFIX$it", locale = locale)
                )
            }
        )
    }
}
