package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.ParticipantModel
import java.util.UUID
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.IdReferenceDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ParticipantWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [CustomDateTimeWriterDtoMapper::class, IdReferenceDtoMapper::class])
interface ParticipantWriterDtoMapper: IGenericProjectWriterDtoMapper<ParticipantModel, ParticipantWriterDto> {
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "firstName", source = "dto.firstName")
	@Mapping(target = "lastName", source = "dto.lastName")
	@Mapping(target = "birthday", source = "dto.birthday")
	@Mapping(target = "type", constant = "REGISTERED")
	@Mapping(target = "startAvailability", source = "dto.startAvailability")
	@Mapping(target = "endAvailability", source = "dto.endAvailability")
	@Mapping(target = "user", source = "dto.userId")
	@Mapping(target = "groups", source = "dto.groupIds")
	@Mapping(target = "projectId", source = "projectId")
	override fun toModel(dto: ParticipantWriterDto, projectId: UUID): ParticipantModel
}
