package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AlertLastCommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AlertReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper

@Mapper(config = DtoMapperConfig::class, uses = [AlertStatusReaderDtoMapper::class, AlertCriticalityReaderDtoMapper::class])
interface AlertReaderDtoMapper: IGenericReaderDtoMapper<AlertModel, AlertReaderDto> {
	override fun toDto(model: AlertModel): AlertReaderDto

	fun toLastCommunication(model: CommunicationModel): AlertLastCommunicationReaderDto
}
