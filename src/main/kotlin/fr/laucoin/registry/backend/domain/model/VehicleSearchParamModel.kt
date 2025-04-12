package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.UsableElementStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.UsableElementStatusEnum.Companion.isAvailable
import fr.laucoin.registry.backend.domain.enumeration.UsableElementStatusEnum.Companion.isPresent
import java.time.ZonedDateTime

data class VehicleSearchParamModel(
    var textSearched: String? = null,
    var visibilitySearched: Boolean? = null,
    var availabilitySearched: Boolean? = null,
    var presenceSearched: Boolean? = null,
    var dateTimeSearched: ZonedDateTime? = null,
) {
    constructor(
        textSearched: String? = null,
        visibilitySearched: Boolean? = null,
        statusSearched: UsableElementStatusEnum? = null,
        dateTimeSearched: ZonedDateTime?,
    ): this(
        textSearched = textSearched,
        visibilitySearched = visibilitySearched,
        availabilitySearched = statusSearched.isAvailable(),
        presenceSearched = statusSearched.isPresent(),
        dateTimeSearched = dateTimeSearched,
    )
}
