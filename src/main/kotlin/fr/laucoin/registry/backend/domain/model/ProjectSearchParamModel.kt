package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime

data class ProjectSearchParamModel(
	var isVisible: Boolean? = null,
	var at: ZonedDateTime? = null,
	var sort: SortModel? = null,
) {
	var query: String? = null

	constructor(
		query: String? = null,
		isVisible: Boolean? = null,
		at: ZonedDateTime? = null,
		sort: SortModel? = null,
	): this(isVisible, at, sort = sort) {
		this.query = if (query.isNullOrBlank()) null else query
	}

	companion object {
		val SORT_FIELDS = setOf(
			"name", "begin", "end",
		)
	}
}
