package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PageReaderDto
import org.springframework.stereotype.Component

/**
 * Wraps a paginated [PageModel] (a domain model) into a [PageReaderDto], so
 * v2 controllers never return a domain model directly on the wire the way
 * v1's `Mono<PageModel<T>>` responses still do.
 */
@Component
class PageReaderDtoMapper {
	fun <M, D> toDto(page: PageModel<M>, itemMapper: (M) -> D): PageReaderDto<D> = PageReaderDto(
		pageNumber = page.pageNumber,
		pageSize = page.pageSize,
		totalPages = page.totalPages,
		totalElements = page.totalElements,
		content = page.content.map(itemMapper),
		lastRefresh = page.lastRefresh,
	)
}
