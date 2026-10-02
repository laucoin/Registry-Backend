package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime

data class GroupSearchParamModel(
	var isVisible: Boolean? = null,
	var isPresent: Boolean? = null,
	var dateTime: ZonedDateTime? = null,
) {
	var query: String? = null

	constructor(
		query: String? = null,
		isVisible: Boolean? = null,
		isPresent: Boolean? = null,
		dateTime: ZonedDateTime? = null,
	): this(isVisible, isPresent, dateTime) {
		this.query = if (query.isNullOrBlank()) null else query
	}
}
