package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.AuditActionEnum
import fr.laucoin.registry.backend.domain.enumeration.AuditOutcomeEnum
import fr.laucoin.registry.backend.domain.enumeration.AuditResourceTypeEnum
import java.time.ZonedDateTime
import java.util.UUID

data class AuditModel(
	val action: AuditActionEnum,
	val resourceType: AuditResourceTypeEnum,
	val resourceId: UUID?,
	val projectId: UUID?,
	val actorUserId: UUID?,
	val outcome: AuditOutcomeEnum,
	val client: ClientContextModel,
	val occurredAt: ZonedDateTime,
)
