package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import java.time.LocalDate
import java.util.UUID

data class GroupContentEntity(
	var id: UUID? = null,
	var groupId: UUID? = null,
	var participantId: UUID? = null,
	var participantFirstName: String? = null,
	var participantLastName: String? = null,
	var participantBirthday: LocalDate? = null,
	var participantType: ParticipantTypeEnum? = null,
)
