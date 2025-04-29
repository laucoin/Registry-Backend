package fr.laucoin.registry.backend.domain.enumeration

import fr.laucoin.registry.backend.domain.constant.ProjectOptionsConst.optionsRules

enum class ProjectOptionEnum {
    ACTIVITY,
    PHONE_COMMUNICATION,
    ACTIVITY_COMMUNICATION,
    SMOKE_REPORT,
    MOVEMENT_REPORT,
    TICKETING,
    VEHICLE,
    FIRE_RISK;

    companion object {
        fun List<ProjectOptionEnum>.missingOptions(): Pair<ProjectOptionEnum, List<ProjectOptionEnum>>? {
            this.forEach {
                if (! this.containsAll(optionsRules[it] !!)) {
                    return Pair(it, optionsRules[it] !!.minus(this.toSet()))
                }
            }
            return null
        }
    }
}
