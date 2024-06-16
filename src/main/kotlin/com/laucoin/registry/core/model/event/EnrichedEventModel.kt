package com.laucoin.registry.core.model.event

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY

data class EnrichedEventModel(
    @JsonProperty(access = WRITE_ONLY)
    var address: AddressModel? = null,
): EventModel() {
    override fun filterFields(): List<String?> =
        super.filterFields() + (address?.filterFields() ?: emptyList())
}
