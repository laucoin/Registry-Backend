package fr.laucoin.registry.backend.domain.enumeration

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.GUEST
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED

enum class MovementReasonEnum : IMovementReasonEnum {
	EMERGENCY {
		override val type = IN
		override val participantType = GUEST
	},
	LOGISTICS {
		override val type = IN
		override val participantType = GUEST
	},
	PARTNER_ANIMATION {
		override val type = IN
		override val participantType = GUEST
	},
	VISIT {
		override val type = IN
		override val participantType = GUEST
	},
	SHOPPING {
		override val type = OUT
		override val participantType = REGISTERED
	},
	MEDICAL {
		override val type = OUT
		override val participantType = REGISTERED
	},
	DEFINITIVE_DEPARTURE {
		override val type = OUT
		override val participantType = REGISTERED
	},
	OTHER {
		override val type = OUT
		override val participantType = REGISTERED
	},
}
