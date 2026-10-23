package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.Companion.isAvailable
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.Companion.isPresent
import java.time.ZonedDateTime

data class VehicleSearchParamModel(
	var isVisible: Boolean? = null,
	var isAvailable: Boolean? = null,
	var isPresent: Boolean? = null,
	var at: ZonedDateTime? = null,
	var brand: String? = null,
	var model: String? = null,
	var sort: SortModel? = null,
) {
	var query: String? = null

	constructor(
		query: String? = null,
		isVisible: Boolean? = null,
		status: PresenceStatusEnum? = null,
		at: ZonedDateTime?,
		brand: String? = null,
		model: String? = null,
		sort: SortModel? = null,
	): this(
		isVisible = isVisible,
		isAvailable = status.isAvailable(),
		isPresent = status.isPresent(),
		at = at,
		brand = brand?.takeUnless { it.isBlank() },
		model = model?.takeUnless { it.isBlank() },
		sort = sort,
	) {
		this.query = if (query.isNullOrBlank()) null else query
	}

	companion object {
		val SORT_FIELDS = setOf(
			"licensePlate", "brand", "model", "startAvailability", "endAvailability",
		)
	}
}
