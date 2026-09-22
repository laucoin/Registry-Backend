package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

import java.time.ZonedDateTime

data class PageReaderDto<T>(
	var pageNumber: Int,
	var pageSize: Int,
	var totalPages: Int,
	var totalElements: Long,
	var content: List<T>,
	var lastRefresh: ZonedDateTime,
)
