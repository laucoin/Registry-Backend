package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_OPTION_FORM_ASK_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_OPTION_NAME_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectOptionsReaderDto
import org.springframework.stereotype.Component

@Component
class ProjectOptionsReaderDtoMapper(
	private val translateService: ITranslateService,
): IGenericReaderDtoMapper<ProjectOptionEnum, ProjectOptionsReaderDto> {
	override fun toDto(model: ProjectOptionEnum): ProjectOptionsReaderDto {
		return ProjectOptionsReaderDto(
			value = model,
			label = translateService.getMessage(code = "$PROJECT_OPTION_NAME_PREFIX$model"),
			ask = translateService.getMessage(code = "$PROJECT_OPTION_FORM_ASK_PREFIX$model"),
			preRequired = model.requiredOptions.map {
				LabelDto(
					it.name,
					translateService.getMessage(code = "$PROJECT_OPTION_NAME_PREFIX$it")
				)
			}
		)
	}
}
