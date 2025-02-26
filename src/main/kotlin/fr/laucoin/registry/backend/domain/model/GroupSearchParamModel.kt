package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime

data class GroupSearchParamModel(
    var textSearched: String? = null,
    var visibilitySearched: Boolean? = null,
    var presenceSearched: Boolean? = null,
    var dateTimeSearched: ZonedDateTime? = null,
)
