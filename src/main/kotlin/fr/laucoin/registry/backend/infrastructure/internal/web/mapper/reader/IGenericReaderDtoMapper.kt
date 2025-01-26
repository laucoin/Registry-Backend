package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto
import java.util.Locale

interface IGenericReaderDtoMapper<M, D> {
    fun toDto(model: M, locale: Locale): D

    fun toDtoList(modelList: List<M>, locale: Locale): List<D> {
        return modelList.map { toDto(it, locale) }
    }

    fun toDtoPage(page: PageDto<M>, locale: Locale): PageDto<D> {
        return PageDto(
            page.offset,
            page.limit,
            page.totalElements,
            toDtoList(page.content, locale),
            page.lastRefresh,
        )
    }
}
