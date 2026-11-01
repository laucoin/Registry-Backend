package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime
import java.util.UUID

data class GroupSearchParamModel(
	var isVisible: Boolean? = null,
	var isPresent: Boolean? = null,
	var at: ZonedDateTime? = null,
	var memberUserId: UUID? = null,
	var hasOnSiteMembers: Boolean? = null,
	var sort: SortModel? = null,
) {
	var query: String? = null

	constructor(
		query: String? = null,
		isVisible: Boolean? = null,
		isPresent: Boolean? = null,
		at: ZonedDateTime? = null,
		memberUserId: UUID? = null,
		hasOnSiteMembers: Boolean? = null,
		sort: SortModel? = null,
	): this(isVisible, isPresent, at, memberUserId, hasOnSiteMembers, sort = sort) {
		this.query = if (query.isNullOrBlank()) null else query
	}

	companion object {
		val SORT_FIELDS = setOf(
			"name", "startAvailability", "endAvailability",
		)
	}
}
