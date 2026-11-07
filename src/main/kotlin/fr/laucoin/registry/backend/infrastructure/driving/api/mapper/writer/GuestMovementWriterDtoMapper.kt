package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.MovementModel
import java.util.UUID
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.GuestMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [MovementContentWriterDtoMapper::class])
interface GuestMovementWriterDtoMapper: IGenericProjectWriterDtoMapper<MovementModel, GuestMovementWriterDto> {
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "contentType", constant = "GUEST")
	@Mapping(target = "dateTime", source = "dto.dateTime")
	@Mapping(target = "type", source = "dto.type")
	@Mapping(target = "reason", source = "dto.reason")
	@Mapping(target = "content", source = "dto.content")
	@Mapping(target = "projectId", source = "projectId")
	override fun toModel(dto: GuestMovementWriterDto, projectId: UUID): MovementModel
}
