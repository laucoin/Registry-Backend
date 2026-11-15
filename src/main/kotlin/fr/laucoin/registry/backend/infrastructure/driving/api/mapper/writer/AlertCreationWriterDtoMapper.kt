package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.AlertModel
import java.util.UUID
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.IdReferenceDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.AlertCreationWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [IdReferenceDtoMapper::class])
interface AlertCreationWriterDtoMapper: IGenericProjectWriterDtoMapper<AlertModel, AlertCreationWriterDto> {
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "title", source = "dto.title")
	@Mapping(target = "dateTime", source = "dto.dateTime")
	@Mapping(target = "status", constant = "IN_PROGRESS")
	@Mapping(target = "communications", expression = "java(java.util.List.of(toCommunication(dto, projectId)))")
	@Mapping(target = "projectId", source = "projectId")
	override fun toModel(dto: AlertCreationWriterDto, projectId: UUID): AlertModel

	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "dateTime", source = "dto.dateTime")
	@Mapping(target = "message", source = "dto.message")
	@Mapping(target = "movement", source = "dto.movementId")
	@Mapping(target = "projectId", source = "projectId")
	fun toCommunication(dto: AlertCreationWriterDto, projectId: UUID): CommunicationModel
}
