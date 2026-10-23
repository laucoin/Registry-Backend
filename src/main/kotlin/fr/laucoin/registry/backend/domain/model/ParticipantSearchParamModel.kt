package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.Companion.isAvailable
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.Companion.isPresent
import fr.laucoin.registry.backend.domain.extension.DateExt.requireOrderedRange
import java.time.LocalDate
import java.time.ZonedDateTime

data class ParticipantSearchParamModel(
	var isMajor: Boolean? = null,
	var type: ParticipantTypeEnum? = null,
	var isVisible: Boolean? = null,
	var isAvailable: Boolean? = null,
	var isPresent: Boolean? = null,
	var at: ZonedDateTime? = null,
	var hasUser: Boolean? = null,
	var birthdayFrom: LocalDate? = null,
	var birthdayTo: LocalDate? = null,
	var sort: SortModel? = null,
) {
	init {
		requireOrderedRange(birthdayFrom, birthdayTo)
	}

	var query: String? = null

	constructor(
		query: String? = null,
		isMajor: Boolean? = null,
		type: ParticipantTypeEnum? = null,
		isVisible: Boolean? = null,
		status: PresenceStatusEnum? = null,
		at: ZonedDateTime?,
		hasUser: Boolean? = null,
		birthdayFrom: LocalDate? = null,
		birthdayTo: LocalDate? = null,
		sort: SortModel? = null,
	): this(
		isMajor = isMajor,
		type = type,
		isVisible = isVisible,
		isAvailable = status.isAvailable(),
		isPresent = status.isPresent(),
		at = at,
		hasUser = hasUser,
		birthdayFrom = birthdayFrom,
		birthdayTo = birthdayTo,
		sort = sort,
	) {
		this.query = if (query.isNullOrBlank()) null else query
	}

	companion object {
		val SORT_FIELDS = setOf(
			"firstName", "lastName", "birthday", "type", "startAvailability", "endAvailability",
		)
	}
}
