package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic

import java.time.ZonedDateTime
import java.util.UUID

abstract class GenericEntity(
	var id: UUID? = null,
	var visible: Boolean? = null,

	var createdAt: ZonedDateTime? = null,
	var creatorId: UUID? = null,
	var creatorFirstName: String? = null,
	var creatorLastName: String? = null,
	var creatorEmail: String? = null,

	var lastUpdateAt: ZonedDateTime? = null,
	var lastEditorId: UUID? = null,
	var lastEditorFirstName: String? = null,
	var lastEditorLastName: String? = null,
	var lastEditorEmail: String? = null,

	var fullCount: Long? = null,
)
