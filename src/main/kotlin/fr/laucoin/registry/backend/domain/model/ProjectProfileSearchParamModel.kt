package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.BLOCKED
import java.time.ZonedDateTime
import java.util.Objects

data class ProjectProfileSearchParamModel(
    var textSearched: String? = null,
    var visibilitySearched: Boolean? = null,
    var availabilitySearched: Boolean? = null,
    val statusSearched: List<ProfileStatusEnum> = ProfileStatusEnum.entries.toList(),
    var dateTimeSearched: ZonedDateTime? = null,
) {
    constructor(
        textSearched: String? = null,
        availabilitySearched: Boolean? = null,
        statusSearched: ProfileStatusEnum? = null,
        dateTimeSearched: ZonedDateTime? = null,
    ): this(
        textSearched,
        if (statusSearched === BLOCKED) false else null,
        availabilitySearched,
        if (Objects.nonNull(statusSearched) && statusSearched !! != BLOCKED) listOf(statusSearched) else ProfileStatusEnum.entries.toList(),
        dateTimeSearched,
    )
}
