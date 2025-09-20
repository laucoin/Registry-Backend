package fr.laucoin.registry.backend.domain.enumeration

enum class PresenceStatusEnum {
	UNAVAILABLE,
	OUT,
	IN;

	companion object {
		fun PresenceStatusEnum?.isAvailable(): Boolean? {
			return when (this) {
				IN -> true
				OUT -> true
				UNAVAILABLE -> false
				else -> null
			}
		}

		fun PresenceStatusEnum?.isPresent(): Boolean? {
			return when (this) {
				IN -> true
				OUT -> false
				UNAVAILABLE -> null
				else -> null
			}
		}
	}
}
