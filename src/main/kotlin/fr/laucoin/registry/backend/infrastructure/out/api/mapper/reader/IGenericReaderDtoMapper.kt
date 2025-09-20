package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.PageModel
import java.util.Locale

interface IGenericReaderDtoMapper<M, D> {
	fun toDto(model: M, locale: Locale): D

	fun toDtoList(modelList: List<M>, locale: Locale): List<D> {
		return modelList.map { toDto(it, locale) }
	}

	fun toDtoPage(page: PageModel<M>, locale: Locale): PageModel<D> {
		return PageModel(
			page.pageNumber,
			page.pageSize,
			page.totalPages,
			page.totalElements,
			toDtoList(page.content, locale),
			page.lastRefresh,
		)
	}
}
