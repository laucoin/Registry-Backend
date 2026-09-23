package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime

data class ProjectSearchParamModel(
	var visibilitySearched: Boolean? = null,
	var dateTimeSearched: ZonedDateTime? = null,
	var favoriteSearched: Boolean? = null,
) {
	var textSearched: String? = null

	constructor(
		textSearched: String? = null,
		visibilitySearched: Boolean? = null,
		dateTimeSearched: ZonedDateTime? = null,
		favoriteSearched: Boolean? = null,
	): this(visibilitySearched, dateTimeSearched, favoriteSearched) {
		this.textSearched = if (textSearched.isNullOrBlank()) null else textSearched
	}
}

