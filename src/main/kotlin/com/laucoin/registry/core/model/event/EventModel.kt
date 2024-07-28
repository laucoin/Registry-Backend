package com.laucoin.registry.core.model.event

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY
import com.laucoin.registry.core.model.util.GenericModel
import java.time.LocalDateTime
import java.util.UUID

open class EventModel(
    var name: String? = null,
    @JsonProperty(access = WRITE_ONLY)
    var addressId: UUID? = null,
    var options: List<EventOptionEnum> = emptyList(),
    var startTime: LocalDateTime? = null,
    var endTime: LocalDateTime? = null,
): GenericModel() {
    override fun filterFields(): List<String?> = listOf(name)
}
