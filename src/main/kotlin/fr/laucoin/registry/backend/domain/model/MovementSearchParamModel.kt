package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import java.time.ZonedDateTime
import java.util.Objects

data class MovementSearchParamModel(
	var visibilitySearched: Boolean? = null,
	val linkedToActivity: Boolean? = null,
	val typeSearched: List<MovementTypeEnum> = MovementTypeEnum.entries.toList(),
	var startDateTimeSearched: ZonedDateTime? = null,
	var endDateTimeSearched: ZonedDateTime? = null,
) {
	constructor(
		visibilitySearched: Boolean? = null,
		linkedToActivity: Boolean? = null,
		typeSearched: MovementTypeEnum? = null,
		startDateTimeSearched: ZonedDateTime? = null,
		endDateTimeSearched: ZonedDateTime? = null,
	): this(
		visibilitySearched,
		linkedToActivity,
		if (Objects.nonNull(typeSearched)) listOf(typeSearched!!) else MovementTypeEnum.entries.toList(),
		startDateTimeSearched,
		endDateTimeSearched,
	)

	constructor(): this(null, null, MovementTypeEnum.entries.toList(), null, null)
}
