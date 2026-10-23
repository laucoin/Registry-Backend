package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.CommunicationTargetEnum
import fr.laucoin.registry.backend.domain.extension.DateExt.requireOrderedRange
import java.time.ZonedDateTime

data class CommunicationSearchParamModel(
	var isVisible: Boolean? = null,
	var from: ZonedDateTime? = null,
	var to: ZonedDateTime? = null,
	var target: CommunicationTargetEnum? = null,
	var sort: SortModel? = null,
) {
	init {
		requireOrderedRange(from, to)
	}

	var query: String? = null

	constructor(
		query: String? = null,
		isVisible: Boolean? = null,
		from: ZonedDateTime? = null,
		to: ZonedDateTime? = null,
		target: CommunicationTargetEnum? = null,
		sort: SortModel? = null,
	): this(isVisible, from, to, target, sort = sort) {
		this.query = if (query.isNullOrBlank()) null else query
	}

	companion object {
		val SORT_FIELDS = setOf(
			"dateTime", "message",
		)
	}
}
