package fr.laucoin.registry.backend.domain.enumeration

enum class UsableElementStatusEnum {
    UNAVAILABLE,
    OUT,
    IN;

    companion object {
        fun UsableElementStatusEnum?.isAvailable(): Boolean? {
            return when (this) {
                IN -> true
                OUT -> true
                UNAVAILABLE -> false
                else -> null
            }
        }

        fun UsableElementStatusEnum?.isPresent(): Boolean? {
            return when (this) {
                IN -> true
                OUT -> false
                UNAVAILABLE -> null
                else -> null
            }
        }
    }
}
