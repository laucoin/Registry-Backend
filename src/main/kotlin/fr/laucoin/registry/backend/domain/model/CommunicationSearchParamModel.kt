package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime

data class CommunicationSearchParamModel(
	var isVisible: Boolean? = null,
	var startDateTime: ZonedDateTime? = null,
	var endDateTime: ZonedDateTime? = null,
) {
	var query: String? = null

	constructor(
		query: String? = null,
		isVisible: Boolean? = null,
		startDateTime: ZonedDateTime? = null,
		endDateTime: ZonedDateTime? = null,
	): this(isVisible, startDateTime, endDateTime) {
		this.query = if (query.isNullOrBlank()) null else query
	}
}
