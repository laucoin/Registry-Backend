package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer

import fr.laucoin.registry.backend.domain.extension.StringExt.toNormalizedEmail
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ProjectProfilesWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import java.util.UUID
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [CustomDateTimeWriterDtoMapper::class])
interface ProjectProfilesWriterDtoMapper {
	fun toModels(dto: ProjectProfilesWriterDto, projectId: UUID): List<ProjectProfileModel> {
		return dto.emails!!.map { it.toNormalizedEmail() }.distinct().map { toModel(dto, projectId, it) }
	}

	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "role", source = "dto.role")
	@Mapping(target = "startAccess", source = "dto.startAccess")
	@Mapping(target = "endAccess", source = "dto.endAccess")
	@Mapping(target = "projectId", source = "projectId")
	@Mapping(target = "user", source = "email")
	fun toModel(dto: ProjectProfilesWriterDto, projectId: UUID, email: String): ProjectProfileModel

	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "email", source = "email")
	fun toUser(email: String): UserModel
}
