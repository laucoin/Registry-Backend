package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime
import kotlin.math.ceil

data class PageModel<T>(
	var page: Int,
	var size: Int,
	var totalPages: Int,
	var totalElements: Long = 0,
	var content: List<T> = emptyList(),
	val lastRefresh: ZonedDateTime? = null
) {
	val hasNext: Boolean
		get() = page + 1 < totalPages

	constructor(
		pageable: PageableModel,
		totalElements: Long,
		pageContent: List<T>,
		lastRefresh: ZonedDateTime? = null,
	): this(
		page = (pageable.offset / pageable.limit).toInt(),
		size = pageable.limit,
		totalPages = ceil(totalElements.toDouble() / pageable.limit.toDouble()).toInt(),
		totalElements = totalElements,
		content = pageContent,
		lastRefresh = lastRefresh,
	)
}
