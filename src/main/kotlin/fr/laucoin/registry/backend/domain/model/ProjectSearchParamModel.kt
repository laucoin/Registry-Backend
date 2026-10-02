package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime

data class ProjectSearchParamModel(
	var isVisible: Boolean? = null,
	var dateTime: ZonedDateTime? = null,
	var isFavorite: Boolean? = null,
) {
	var query: String? = null

	constructor(
		query: String? = null,
		isVisible: Boolean? = null,
		dateTime: ZonedDateTime? = null,
		isFavorite: Boolean? = null,
	): this(isVisible, dateTime, isFavorite) {
		this.query = if (query.isNullOrBlank()) null else query
	}
}

