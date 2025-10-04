package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.PageModel

interface IGenericReaderDtoMapper<M, D> {
	fun toDto(model: M): D

	fun toDtoList(modelList: List<M>): List<D> {
		return modelList.map(this::toDto)
	}

	fun toDtoPage(page: PageModel<M>): PageModel<D> {
		return PageModel(
			page.pageNumber,
			page.pageSize,
			page.totalPages,
			page.totalElements,
			toDtoList(page.content),
			page.lastRefresh,
		)
	}
}
