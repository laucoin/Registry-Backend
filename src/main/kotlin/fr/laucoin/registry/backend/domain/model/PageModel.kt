package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime
import java.time.ZonedDateTime.now
import kotlin.math.ceil

data class PageModel<T>(
	var pageNumber: Int,
	var pageSize: Int,
	var totalPages: Int,
	var totalElements: Long = 0,
	var content: List<T> = emptyList(),
	val lastRefresh: ZonedDateTime = now()
) {
	constructor(pageable: PageableModel, totalElements: Long, pageContent: List<T>) : this(
		pageNumber = pageable.offset / pageable.limit,
		pageSize = pageable.limit,
		totalPages = ceil(totalElements.toDouble() / pageable.limit.toDouble()).toInt(),
		totalElements = totalElements,
		content = pageContent,
	)
}
