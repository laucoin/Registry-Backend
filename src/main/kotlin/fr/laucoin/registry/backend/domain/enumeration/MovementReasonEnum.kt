package fr.laucoin.registry.backend.domain.enumeration

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT

enum class MovementReasonEnum: IMovementReasonEnum {
    ARRIVAL {
        override val type = IN
    },
    VISIT {
        override val type = IN
    },
    SHOPPING {
        override val type = OUT
    },
    MEDICAL {
        override val type = OUT
    },
    LOGISTICS {
        override val type = OUT
    },
    FINAL_EXIT {
        override val type = OUT
    },
    OTHER {
        override val type = OUT
    }
}
