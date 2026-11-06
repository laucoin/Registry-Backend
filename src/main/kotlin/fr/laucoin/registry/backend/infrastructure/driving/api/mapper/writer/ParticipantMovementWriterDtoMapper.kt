package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.MovementModel
import java.util.UUID
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.IdReferenceDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ParticipantMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [ParticipantMovementContentWriterDtoMapper::class, IdReferenceDtoMapper::class])
interface ParticipantMovementWriterDtoMapper: IGenericProjectWriterDtoMapper<MovementModel, ParticipantMovementWriterDto> {
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "contentType", constant = "REGISTERED")
	@Mapping(target = "dateTime", source = "dto.dateTime")
	@Mapping(target = "type", source = "dto.type")
	@Mapping(target = "reason", source = "dto.reason")
	@Mapping(target = "activity", source = "dto.activityId")
	@Mapping(target = "content", source = "dto.content")
	@Mapping(target = "projectId", source = "projectId")
	override fun toModel(dto: ParticipantMovementWriterDto, projectId: UUID): MovementModel
}
