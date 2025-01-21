package fr.laucoin.registry.backend.domain.constant

import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.ACTIVITY_COMMUNICATION
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.FIRE_RISK
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.MOVEMENT_REPORT
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.PHONE_COMMUNICATION
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.SMOKE_REPORT
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.TICKETING
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.VEHICLE

object EventOptionsConst {
    val optionsRules: Map<EventOptionEnum, Collection<EventOptionEnum>> = mapOf(
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
