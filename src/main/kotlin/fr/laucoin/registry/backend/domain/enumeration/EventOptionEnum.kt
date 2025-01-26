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
        fun List<EventOptionEnum>.missingOptions(): List<EventOptionEnum> {
            val missingOptions = arrayListOf<EventOptionEnum>()
            this.forEach {
                if (! this.containsAll(optionsRules[it] !!)) {
                    missingOptions.addAll(optionsRules[it] !!.minus(this.toSet()))
                }
            }
            return missingOptions
        }
    }
}
