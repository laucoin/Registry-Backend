package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_OPTION_NAME_PREFIX
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectReaderDto
import java.util.Locale
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class ProjectReaderDtoMapper(
	private val translateService: ITranslateService,
	private val availabilityStatusMapper: AvailabilityStatusReaderDtoMapper
): IGenericReaderDtoMapper<ProjectModel, ProjectReaderDto> {
	override fun toDto(model: ProjectModel, locale: Locale): ProjectReaderDto {
		return ProjectReaderDto(
			name = model.name,
			status = Optional.ofNullable(model.status)
				.map { availabilityStatusMapper.toDto(it, locale, model.begin, model.end) }.orElse(null),
			begin = model.begin,
			end = model.end,
			options = model.options?.map {
				LabelDto(
					it.name,
					translateService.getMessage(code = "$PROJECT_OPTION_NAME_PREFIX$it", locale = locale)
				)
			},
		).apply {
			id = model.id
			visible = model.visible
			creation = model.creation
			lastEdition = model.lastEdition
		}
	}
}
