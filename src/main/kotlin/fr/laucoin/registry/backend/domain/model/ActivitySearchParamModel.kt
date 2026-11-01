package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_DURATION_MIN_HIGHER_THAN_MAX
import java.time.ZonedDateTime
import kotlin.time.Duration
import org.springframework.http.HttpStatus.BAD_REQUEST

data class ActivitySearchParamModel(
	var isVisible: Boolean? = null,
	var isAvailable: Boolean? = null,
	var at: ZonedDateTime? = null,
	var minDuration: Duration? = null,
	var maxDuration: Duration? = null,
	var participants: Int? = null,
	var sort: SortModel? = null,
) {
	init {
		requireOrderedDuration()
	}

	var query: String? = null

	constructor(
		query: String? = null,
		isVisible: Boolean? = null,
		isAvailable: Boolean? = null,
		at: ZonedDateTime? = null,
		minDuration: Duration? = null,
		maxDuration: Duration? = null,
		participants: Int? = null,
		sort: SortModel? = null,
	): this(isVisible, isAvailable, at, minDuration, maxDuration, participants, sort) {
		this.query = if (query.isNullOrBlank()) null else query
	}

	private fun requireOrderedDuration() {
		val min = minDuration
		val max = maxDuration
		if (min != null && max != null && min > max) {
			throw RegistryException(BAD_REQUEST, ACTIVITY_DURATION_MIN_HIGHER_THAN_MAX)
		}
	}

	companion object {
		val SORT_FIELDS = setOf(
			"name", "description", "duration", "minAllowedParticipants", "maxAllowedParticipants",
			"startAvailability", "endAvailability",
		)
	}
}
