package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import java.time.ZonedDateTime

data class ParticipantSearchParamModel(
	var isMajor: Boolean? = null,
	var typeSearched: ParticipantTypeEnum? = null,
	var visibilitySearched: Boolean? = null,
	var availabilitySearched: Boolean? = null,
	var presenceStatusSearched: PresenceStatusEnum? = null,
	var departedSearched: Boolean? = null,
	var warnedSearched: Boolean? = null,
	var dateTimeSearched: ZonedDateTime? = null,
	var groupedSearched: Boolean? = null,
) {
	var textSearched: String? = null

	/**
	 * A status now travels to the query whole, because the four states it can take
	 * are no longer a pair of booleans: `DEPARTED` and `UNAVAILABLE` both mean "not
	 * counted", for reasons the register must keep apart, and `IN`/`OUT` no longer
	 * imply availability at all. `availableSearched` stays the coarser "the window
	 * contains now" the list exposes on its own. Both are named apart from the
	 * properties they feed: the two constructors would otherwise offer the same
	 * parameter names and no call could resolve.
	 */
	constructor(
		textSearched: String? = null,
		isMajor: Boolean? = null,
		typeSearched: ParticipantTypeEnum? = null,
		visibilitySearched: Boolean? = null,
		statusSearched: PresenceStatusEnum? = null,
		dateTimeSearched: ZonedDateTime?,
		availableSearched: Boolean? = null,
		departedSearched: Boolean? = null,
		warnedSearched: Boolean? = null,
		groupedSearched: Boolean? = null,
	) : this(
		isMajor = isMajor,
		typeSearched = typeSearched,
		visibilitySearched = visibilitySearched,
		availabilitySearched = availableSearched,
		presenceStatusSearched = statusSearched,
		departedSearched = departedSearched,
		warnedSearched = warnedSearched,
		dateTimeSearched = dateTimeSearched,
		groupedSearched = groupedSearched,
	) {
		this.textSearched = if (textSearched.isNullOrBlank()) null else textSearched
	}
}
