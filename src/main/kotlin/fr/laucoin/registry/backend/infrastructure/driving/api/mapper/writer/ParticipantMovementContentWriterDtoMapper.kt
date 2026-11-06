package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.IdReferenceDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ParticipantMovementWriterDto.ParticipantMovementContentWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [IdReferenceDtoMapper::class])
interface ParticipantMovementContentWriterDtoMapper: IGenericWriterDtoMapper<MovementContentModel, ParticipantMovementContentWriterDto> {
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "poolName", source = "dto.poolName")
	@Mapping(target = "participant", source = "dto.participantId")
	@Mapping(target = "vehicle", source = "dto.vehicleId")
	override fun toModel(dto: ParticipantMovementContentWriterDto): MovementContentModel
}
