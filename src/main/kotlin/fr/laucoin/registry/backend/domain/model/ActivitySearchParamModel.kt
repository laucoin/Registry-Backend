package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime

data class ActivitySearchParamModel(
    var visibilitySearched: Boolean? = null,
    var availabilitySearched: Boolean? = null,
    var dateTimeSearched: ZonedDateTime? = null,
) {
    var textSearched: String? = null

    constructor(
        textSearched: String? = null,
        visibilitySearched: Boolean? = null,
        availabilitySearched: Boolean? = null,
        dateTimeSearched: ZonedDateTime? = null,
    ): this(visibilitySearched, availabilitySearched, dateTimeSearched) {
        this.textSearched = if (textSearched.isNullOrBlank()) null else textSearched
    }
}

