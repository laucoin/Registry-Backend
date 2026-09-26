package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_OPTION_NAME_PREFIX
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectReaderDto
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class ProjectReaderDtoMapper(
	private val translateService: ITranslateService,
	private val availabilityStatusMapper: AvailabilityStatusReaderDtoMapper,
	private val profileMapper: ProjectProfileReaderDtoMapper,
	private val countMapper: ProjectCountReaderDtoMapper,
) : IGenericReaderDtoMapper<ProjectModel, ProjectReaderDto> {
	override fun toDto(model: ProjectModel): ProjectReaderDto {
		return ProjectReaderDto(
			name = model.name,
			status = Optional.ofNullable(model.status)
				.map { availabilityStatusMapper.toDto(it, model.begin, model.end) }.orElse(null),
			begin = model.begin,
			end = model.end,
			options = model.options?.map {
				LabelDto(
					it.name,
					translateService.getMessage(code = "$PROJECT_OPTION_NAME_PREFIX$it")
				)
			},
			counts = model.counts?.let(countMapper::toDto),
			activeProfile = Optional.ofNullable(model.activeProfile).map(profileMapper::toDto).orElse(null),
		).apply {
			id = model.id
			visible = model.visible
			creation = model.creation
			lastEdition = model.lastEdition
		}
	}
}
