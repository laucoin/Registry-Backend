package fr.laucoin.registry.backend.domain.enumeration

import fr.laucoin.registry.backend.domain.constant.EventOptionsConst.optionsRules

enum class EventOptionEnum {
    ACTIVITY,
    PHONE_COMMUNICATION,
    ACTIVITY_COMMUNICATION,
    SMOKE_REPORT,
    MOVEMENT_REPORT,
    TICKETING,
    VEHICLE,
    FIRE_RISK;

    companion object {
        fun List<EventOptionEnum>.missingOptions(): Pair<EventOptionEnum, List<EventOptionEnum>>? {
            this.forEach {
                if (! this.containsAll(optionsRules[it] !!)) {
                    return Pair(it, optionsRules[it] !!.minus(this.toSet()))
                }
            }
            return null
        }
    }
}
