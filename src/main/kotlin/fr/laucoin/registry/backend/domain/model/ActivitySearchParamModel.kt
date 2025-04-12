package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime

data class ActivitySearchParamModel(
    var textSearched: String? = null,
    var visibilitySearched: Boolean? = null,
    var availabilitySearched: Boolean? = null,
    var dateTimeSearched: ZonedDateTime? = null,
)
