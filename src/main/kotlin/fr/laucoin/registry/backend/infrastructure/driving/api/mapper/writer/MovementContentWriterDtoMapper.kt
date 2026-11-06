package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.IdReferenceDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.MovementContentWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [IdReferenceDtoMapper::class])
interface MovementContentWriterDtoMapper: IGenericWriterDtoMapper<MovementContentModel, MovementContentWriterDto> {
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "participant", source = "dto.participantId")
	override fun toModel(dto: MovementContentWriterDto): MovementContentModel
}
