package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime

data class ProjectSearchParamModel(
    var textSearched: String? = null,
    var visibilitySearched: Boolean? = null,
    var dateTimeSearched: ZonedDateTime? = null,
)
