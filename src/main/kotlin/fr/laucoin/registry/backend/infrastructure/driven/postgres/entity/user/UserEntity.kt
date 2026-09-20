package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user

import fr.laucoin.registry.backend.domain.enumeration.UserTypeEnum
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericEntity
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

open class UserEntity(
	var oidcId: UUID? = null,
	var type: UserTypeEnum? = null,
	var firstName: String? = null,
	var lastName: String? = null,
	var email: String? = null,
	var role: String? = null,
	var birthday: LocalDate? = null,
	var lastLogin: ZonedDateTime? = null,
	var purged: Boolean? = null,
): GenericEntity()

