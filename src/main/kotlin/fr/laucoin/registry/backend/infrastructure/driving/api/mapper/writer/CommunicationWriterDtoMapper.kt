package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.CommunicationModel
import java.util.UUID
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.IdReferenceDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.CommunicationWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [IdReferenceDtoMapper::class])
interface CommunicationWriterDtoMapper: IGenericProjectWriterDtoMapper<CommunicationModel, CommunicationWriterDto> {
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "dateTime", source = "dto.dateTime")
	@Mapping(target = "message", source = "dto.message")
	@Mapping(target = "movement", source = "dto.movementId")
	@Mapping(target = "alert", source = "dto.alertId")
	@Mapping(target = "projectId", source = "projectId")
	override fun toModel(dto: CommunicationWriterDto, projectId: UUID): CommunicationModel
}
