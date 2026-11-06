package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.ProfileLabelMappingHelper
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.StatusLabelMappingHelper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [PartialUserReaderDtoMapper::class, ProjectProfileRoleReaderDtoMapper::class, StatusLabelMappingHelper::class, ProfileLabelMappingHelper::class])
interface ProjectProfileReaderDtoMapper: IGenericReaderDtoMapper<ProjectProfileModel, ProjectProfileReaderDto> {
	@Mapping(target = "availabilityStatus", source = "model", qualifiedByName = ["profileAvailabilityStatus"])
	@Mapping(target = "status", source = "model", qualifiedByName = ["profileStatus"])
	override fun toDto(model: ProjectProfileModel): ProjectProfileReaderDto
}
