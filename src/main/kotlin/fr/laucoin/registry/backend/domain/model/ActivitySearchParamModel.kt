package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime

data class ActivitySearchParamModel(
	var isVisible: Boolean? = null,
	var isAvailable: Boolean? = null,
	var dateTime: ZonedDateTime? = null,
) {
	var query: String? = null

	constructor(
		query: String? = null,
		isVisible: Boolean? = null,
		isAvailable: Boolean? = null,
		dateTime: ZonedDateTime? = null,
	): this(isVisible, isAvailable, dateTime) {
		this.query = if (query.isNullOrBlank()) null else query
	}
}

