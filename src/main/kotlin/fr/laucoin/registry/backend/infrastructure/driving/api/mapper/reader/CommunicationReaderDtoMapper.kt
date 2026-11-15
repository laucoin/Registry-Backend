package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper

@Mapper(config = DtoMapperConfig::class, uses = [MovementReaderDtoMapper::class, AlertReaderDtoMapper::class])
interface CommunicationReaderDtoMapper: IGenericReaderDtoMapper<CommunicationModel, CommunicationReaderDto> {
	override fun toDto(model: CommunicationModel): CommunicationReaderDto
}
