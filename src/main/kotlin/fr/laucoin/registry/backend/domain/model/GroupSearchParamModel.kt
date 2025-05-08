package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime

data class GroupSearchParamModel(
    var visibilitySearched: Boolean? = null,
    var presenceSearched: Boolean? = null,
    var dateTimeSearched: ZonedDateTime? = null,
) {
    var textSearched: String? = null

    constructor(
        textSearched: String? = null,
        visibilitySearched: Boolean? = null,
        presenceSearched: Boolean? = null,
        dateTimeSearched: ZonedDateTime? = null,
    ): this(visibilitySearched, presenceSearched, dateTimeSearched) {
        this.textSearched = if (textSearched.isNullOrBlank()) null else textSearched
    }
}
