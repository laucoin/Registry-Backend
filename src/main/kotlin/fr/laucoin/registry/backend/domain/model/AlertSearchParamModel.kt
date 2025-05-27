package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import java.time.ZonedDateTime
import java.util.Objects

data class AlertSearchParamModel(
    var visibilitySearched: Boolean? = null,
    val statusSearched: List<AlertStatusEnum> = AlertStatusEnum.entries.toList(),
    var startDateTimeSearched: ZonedDateTime? = null,
    var endDateTimeSearched: ZonedDateTime? = null,
) {
    var textSearched: String? = null

    constructor(
        textSearched: String? = null,
        visibilitySearched: Boolean? = null,
        statusSearched: AlertStatusEnum? = null,
        startDateTimeSearched: ZonedDateTime? = null,
        endDateTimeSearched: ZonedDateTime? = null,
    ): this(
        visibilitySearched,
        if (Objects.nonNull(statusSearched)) listOf(statusSearched !!) else AlertStatusEnum.entries.toList(),
        startDateTimeSearched,
        endDateTimeSearched
    ) {
        this.textSearched = if (textSearched.isNullOrBlank()) null else textSearched
    }
}
