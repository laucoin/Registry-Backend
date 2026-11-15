package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query

import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.PageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class)
interface PageQueryDtoMapper {
	@Mapping(target = "offset", expression = "java(dto.getPage() * dto.getSize())")
	@Mapping(target = "limit", source = "size")
	fun toPageable(dto: PageQueryDto): PageableModel
}
