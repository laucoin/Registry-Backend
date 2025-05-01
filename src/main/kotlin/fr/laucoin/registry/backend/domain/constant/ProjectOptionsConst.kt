package fr.laucoin.registry.backend.domain.constant

import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.ACTIVITY_COMMUNICATION
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.FIRE_RISK
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.MOVEMENT_REPORT
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.PHONE_COMMUNICATION
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.SMOKE_REPORT
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.TICKETING
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.VEHICLE

object ProjectOptionsConst {
    val optionsRules: Map<ProjectOptionEnum, Collection<ProjectOptionEnum>> = mapOf(
        TICKETING to emptyList(),
        VEHICLE to emptyList(),
        FIRE_RISK to emptyList(),
        PHONE_COMMUNICATION to emptyList(),
        ACTIVITY to emptyList(),
        ACTIVITY_COMMUNICATION to listOf(ACTIVITY),
        SMOKE_REPORT to listOf(ACTIVITY_COMMUNICATION),
        MOVEMENT_REPORT to listOf(ACTIVITY_COMMUNICATION),
    )
}
