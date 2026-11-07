package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.GroupModel
import java.util.UUID
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.IdReferenceDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.GroupWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [CustomDateTimeWriterDtoMapper::class, IdReferenceDtoMapper::class])
interface GroupWriterDtoMapper: IGenericProjectWriterDtoMapper<GroupModel, GroupWriterDto> {
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "name", source = "dto.name")
	@Mapping(target = "startAvailability", source = "dto.startAvailability")
	@Mapping(target = "endAvailability", source = "dto.endAvailability")
	@Mapping(target = "members", source = "dto.members")
	@Mapping(target = "projectId", source = "projectId")
	override fun toModel(dto: GroupWriterDto, projectId: UUID): GroupModel
}
