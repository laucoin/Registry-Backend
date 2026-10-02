package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import java.time.ZonedDateTime
import java.util.Objects

data class AlertSearchParamModel(
	var isVisible: Boolean? = null,
	val status: List<AlertStatusEnum> = AlertStatusEnum.entries.toList(),
	var startDateTime: ZonedDateTime? = null,
	var endDateTime: ZonedDateTime? = null,
) {
	var query: String? = null

	constructor(
		query: String? = null,
		isVisible: Boolean? = null,
		status: AlertStatusEnum? = null,
		startDateTime: ZonedDateTime? = null,
		endDateTime: ZonedDateTime? = null,
	): this(
		isVisible,
		if (Objects.nonNull(status)) listOf(status!!) else AlertStatusEnum.entries.toList(),
		startDateTime,
		endDateTime
	) {
		this.query = if (query.isNullOrBlank()) null else query
	}
}
