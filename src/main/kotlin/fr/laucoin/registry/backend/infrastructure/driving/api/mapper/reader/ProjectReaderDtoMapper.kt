package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.model.ProjectCountsModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectCountsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.StatusLabelMappingHelper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.AfterMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget

@Mapper(config = DtoMapperConfig::class, uses = [StatusLabelMappingHelper::class, ProjectOptionLabelReaderDtoMapper::class])
interface ProjectReaderDtoMapper: IGenericReaderDtoMapper<ProjectModel, ProjectReaderDto> {
	@Mapping(target = "status", source = "model", qualifiedByName = ["projectStatus"])
	@Mapping(target = "options", ignore = true)
	override fun toDto(model: ProjectModel): ProjectReaderDto

	fun toCounts(counts: ProjectCountsModel): ProjectCountsReaderDto

	fun toOptionLabels(options: List<ProjectOptionEnum>?): List<LabelDto>?

	@AfterMapping
	fun fillOptions(model: ProjectModel, @MappingTarget dto: ProjectReaderDto) {
		dto.options = toOptionLabels(model.options)
	}
}
