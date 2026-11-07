package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.UserInvitationReaderDto.InvitationProjectReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.UserInvitationReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.ProfileLabelMappingHelper
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.StatusLabelMappingHelper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [StatusLabelMappingHelper::class, ProfileLabelMappingHelper::class, ProjectProfileRoleReaderDtoMapper::class])
interface UserInvitationReaderDtoMapper: IGenericReaderDtoMapper<ProjectProfileModel, UserInvitationReaderDto> {
	@Mapping(target = "availabilityStatus", source = "model", qualifiedByName = ["profileAvailabilityStatus"])
	@Mapping(target = "status", source = "model", qualifiedByName = ["profileStatus"])
	override fun toDto(model: ProjectProfileModel): UserInvitationReaderDto

	fun toInvitationProject(model: ProjectModel): InvitationProjectReaderDto
}
