package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AuditPurgeReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class)
interface AuditPurgeReaderDtoMapper: IGenericReaderDtoMapper<Long?, AuditPurgeReaderDto> {
	@Mapping(target = "count", source = "model")
	@Mapping(target = "copy", ignore = true)
	override fun toDto(model: Long?): AuditPurgeReaderDto
}
