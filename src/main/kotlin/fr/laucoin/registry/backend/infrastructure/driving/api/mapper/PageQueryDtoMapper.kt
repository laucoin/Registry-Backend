package fr.laucoin.registry.backend.infrastructure.driving.api.mapper

import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.PageQueryDto
import org.springframework.stereotype.Component

@Component
class PageQueryDtoMapper {
	fun toPageable(query: PageQueryDto): PageableModel = PageableModel(query.page * query.size, query.size)
}
