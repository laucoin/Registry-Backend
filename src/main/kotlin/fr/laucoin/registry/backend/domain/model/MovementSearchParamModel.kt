package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import java.time.ZonedDateTime
import java.util.Objects

data class MovementSearchParamModel(
	var isVisible: Boolean? = null,
	val hasActivity: Boolean? = null,
	val type: List<MovementTypeEnum> = MovementTypeEnum.entries.toList(),
	var startDateTime: ZonedDateTime? = null,
	var endDateTime: ZonedDateTime? = null,
) {
	constructor(
		isVisible: Boolean? = null,
		hasActivity: Boolean? = null,
		type: MovementTypeEnum? = null,
		startDateTime: ZonedDateTime? = null,
		endDateTime: ZonedDateTime? = null,
	): this(
		isVisible,
		hasActivity,
		if (Objects.nonNull(type)) listOf(type!!) else MovementTypeEnum.entries.toList(),
		startDateTime,
		endDateTime,
	)

	constructor(): this(null, null, MovementTypeEnum.entries.toList(), null, null)
}
