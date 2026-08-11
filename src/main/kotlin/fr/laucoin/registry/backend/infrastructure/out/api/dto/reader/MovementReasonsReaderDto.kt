package fr.laucoin.registry.backend.infrastructure.out.api.dto.reader

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonKindEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum

class MovementReasonsReaderDto(
	val value: String?,
	val label: String,
	val kind: MovementReasonKindEnum,
	val type: MovementTypeEnum? = null,
	/**
	 * How long the linked activity was expected to take, as an ISO-8601 duration
	 * — only ever set on an ACTIVITY reason, and only when that activity states
	 * one. It rides along on the movement so a caller can tell an outing that has
	 * overrun from one that has not without asking for the activity: the
	 * dashboard used to fetch every listed activity one by one for exactly this.
	 */
	val duration: String? = null,
)
