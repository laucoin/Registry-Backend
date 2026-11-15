package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.model.ProjectCountsModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectCountsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectCurrentProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectWithProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.ProfileLabelMappingHelper
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.StatusLabelMappingHelper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.AfterMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget

@Mapper(config = DtoMapperConfig::class, uses = [StatusLabelMappingHelper::class, ProjectOptionLabelReaderDtoMapper::class, ProjectProfileRoleReaderDtoMapper::class, ProfileLabelMappingHelper::class])
interface ProjectWithProfileReaderDtoMapper: IGenericReaderDtoMapper<ProjectModel, ProjectWithProfileReaderDto> {
	@Mapping(target = "status", source = "model", qualifiedByName = ["projectStatus"])
	@Mapping(target = "options", ignore = true)
	override fun toDto(model: ProjectModel): ProjectWithProfileReaderDto

	@Mapping(target = "availabilityStatus", source = "model", qualifiedByName = ["profileAvailabilityStatus"])
	@Mapping(target = "status", source = "model", qualifiedByName = ["profileStatus"])
	@Mapping(target = "creation", ignore = true)
	@Mapping(target = "lastEdition", ignore = true)
	fun toCurrentProfile(model: ProjectProfileModel): ProjectCurrentProfileReaderDto

	fun toCounts(counts: ProjectCountsModel): ProjectCountsReaderDto

	fun toOptionLabels(options: List<ProjectOptionEnum>?): List<LabelDto>?

	@AfterMapping
	fun fillOptions(model: ProjectModel, @MappingTarget dto: ProjectWithProfileReaderDto) {
		dto.options = toOptionLabels(model.options)
	}
}
