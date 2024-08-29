package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum
import java.time.ZonedDateTime

data class EventModel(
    var name: String? = null,
    var begin: ZonedDateTime? = null,
    var end: ZonedDateTime? = null,
    var options: List<EventOptionEnum>? = emptyList(),
): GenericModel() {
    override fun getSearchableValues(): List<String> = listOfNotNull(name)
}
