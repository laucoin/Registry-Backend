package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime
import java.time.ZonedDateTime.now
import kotlin.math.ceil

data class PageModel<T>(
	var page: Int,
	var size: Int,
	var totalPages: Int,
	var totalElements: Long = 0,
	var content: List<T> = emptyList(),
	val lastRefresh: ZonedDateTime = now()
) {
	val hasNext: Boolean
		get() = page + 1 < totalPages

	constructor(pageable: PageableModel, totalElements: Long, pageContent: List<T>): this(
		page = pageable.offset / pageable.limit,
		size = pageable.limit,
		totalPages = ceil(totalElements.toDouble() / pageable.limit.toDouble()).toInt(),
		totalElements = totalElements,
		content = pageContent,
	)
}
