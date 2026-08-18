package fr.laucoin.registry.backend.infrastructure.out.api.mapper

import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.PageQueryDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.SortedPageQueryDto

object PageQueryDtoMapper {
	fun toPageable(query: PageQueryDto): PageableModel = PageableModel(query.page * query.size, query.size)

	fun <T> toSortModels(query: SortedPageQueryDto, fieldResolver: (String) -> T?): List<SortModel<T>> =
		SortParamDtoMapper.toSortModels(query.sort, query.direction, fieldResolver)
}
