package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.ParticipantModel
import java.util.UUID
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.GuestMovementWriterDto.GuestWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class)
interface GuestWriterDtoMapper: IGenericProjectWriterDtoMapper<ParticipantModel, GuestWriterDto> {
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "id", source = "dto.id")
	@Mapping(target = "firstName", source = "dto.firstName")
	@Mapping(target = "lastName", source = "dto.lastName")
	@Mapping(target = "birthday", source = "dto.birthday")
	@Mapping(target = "type", constant = "GUEST")
	@Mapping(target = "projectId", source = "projectId")
	override fun toModel(dto: GuestWriterDto, projectId: UUID): ParticipantModel
}
