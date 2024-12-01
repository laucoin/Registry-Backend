package fr.laucoin.registry.backend.domain.enumeration

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
        fun List<EventOptionEnum>.isMissingActivity(): Boolean {
            return this.contains(ACTIVITY_COMMUNICATION) && ! this.contains(ACTIVITY)
        }

        fun List<EventOptionEnum>.isMissingActivityCommunication(): Boolean {
            return (this.contains(SMOKE_REPORT) || this.contains(MOVEMENT_REPORT)) && ! this.contains(ACTIVITY_COMMUNICATION)
        }
    }
}
