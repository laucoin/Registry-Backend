package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum

data class EventModel(
    var name: String? = null,
    var begin: CustomDateTimeModel? = null,
    var end: CustomDateTimeModel? = null,
    var options: List<EventOptionEnum>? = emptyList(),
): GenericModel()
