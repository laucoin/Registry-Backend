package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericProjectEntity
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.util.UUID

data class ProjectProfileEntity(
	var userId: UUID? = null,
	var userFirstName: String? = null,
	var userLastName: String? = null,
	var userEmail: String? = null,
	var userLastLogin: ZonedDateTime? = null,
	var userPurged: Boolean? = null,
	var userOidcId: UUID? = null,

	var role: String? = null,
	var status: ProfileStatusEnum? = null,
	var startAccessDate: LocalDate? = null,
	var startAccessTime: OffsetTime? = null,
	var endAccessDate: LocalDate? = null,
	var endAccessTime: OffsetTime? = null,
): GenericProjectEntity()
