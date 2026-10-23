package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime

data class GroupSearchParamModel(
	var isVisible: Boolean? = null,
	var isPresent: Boolean? = null,
	var at: ZonedDateTime? = null,
	var sort: SortModel? = null,
) {
	var query: String? = null

	constructor(
		query: String? = null,
		isVisible: Boolean? = null,
		isPresent: Boolean? = null,
		at: ZonedDateTime? = null,
		sort: SortModel? = null,
	): this(isVisible, isPresent, at, sort = sort) {
		this.query = if (query.isNullOrBlank()) null else query
	}

	companion object {
		val SORT_FIELDS = setOf(
			"name", "startAvailability", "endAvailability",
		)
	}
}
