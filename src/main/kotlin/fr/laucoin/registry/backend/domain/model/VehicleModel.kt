package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime

data class VehicleModel(
    var registration: String? = null,
    var brand: String? = null,
    var model: String? = null,
    var begin: ZonedDateTime? = null,
    var end: ZonedDateTime? = null,
): GenericEventModel() {
    override fun getSearchableValues(): List<String> = listOfNotNull(registration, brand, model)
}
