package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.extension.DateExt.requireOrderedRange
import java.time.ZonedDateTime
import java.util.Objects
import java.util.UUID

data class MovementSearchParamModel(
	var isVisible: Boolean? = null,
	val hasActivity: Boolean? = null,
	val activityId: UUID? = null,
	val type: List<MovementTypeEnum> = MovementTypeEnum.entries.toList(),
	var from: ZonedDateTime? = null,
	var to: ZonedDateTime? = null,
	var sort: SortModel? = null,
) {
	init {
		requireOrderedRange(from, to)
	}

	constructor(
		isVisible: Boolean? = null,
		hasActivity: Boolean? = null,
		type: MovementTypeEnum? = null,
		from: ZonedDateTime? = null,
		to: ZonedDateTime? = null,
		activityId: UUID? = null,
		sort: SortModel? = null,
	): this(
		isVisible,
		hasActivity,
		activityId,
		if (Objects.nonNull(type)) listOf(type!!) else MovementTypeEnum.entries.toList(),
		from,
		to,
		sort = sort,
	)

	constructor(): this(null, null, null, MovementTypeEnum.entries.toList(), null, null)

	companion object {
		val SORT_FIELDS = setOf(
			"dateTime", "type",
		)
	}
}
