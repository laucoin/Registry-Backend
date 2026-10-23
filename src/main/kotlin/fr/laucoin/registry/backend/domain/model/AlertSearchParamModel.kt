package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.extension.DateExt.requireOrderedRange
import java.time.ZonedDateTime
import java.util.Objects

data class AlertSearchParamModel(
	var isVisible: Boolean? = null,
	val status: List<AlertStatusEnum> = AlertStatusEnum.entries.toList(),
	var from: ZonedDateTime? = null,
	var to: ZonedDateTime? = null,
	var sort: SortModel? = null,
) {
	init {
		requireOrderedRange(from, to)
	}

	var query: String? = null

	constructor(
		query: String? = null,
		isVisible: Boolean? = null,
		status: AlertStatusEnum? = null,
		from: ZonedDateTime? = null,
		to: ZonedDateTime? = null,
		sort: SortModel? = null,
	): this(
		isVisible,
		if (Objects.nonNull(status)) listOf(status!!) else AlertStatusEnum.entries.toList(),
		from,
		to,
		sort = sort,
	) {
		this.query = if (query.isNullOrBlank()) null else query
	}

	companion object {
		val SORT_FIELDS = setOf(
			"title", "dateTime", "status",
		)
	}
}
