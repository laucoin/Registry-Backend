package fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user

import fr.laucoin.registry.backend.domain.enumeration.UserTypeEnum
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_BIRTHDAY
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_LAST_LOGIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_OIDC_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_ROLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_TYPE
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@Table(USER_TABLE)
open class UserEntity(
	@Column(USER_OIDC_ID)
	var oidcId: UUID? = null,
	@Column(USER_TYPE)
	var type: UserTypeEnum? = null,
	@Column(USER_FIRST_NAME)
	var firstName: String? = null,
	@Column(USER_LAST_NAME)
	var lastName: String? = null,
	@Column(USER_EMAIL)
	var email: String? = null,
	@Column(USER_ROLE)
	var role: String? = null,
	@Column(USER_BIRTHDAY)
	var birthday: LocalDate? = null,
	@Column(USER_LAST_LOGIN)
	var lastLogin: ZonedDateTime? = null,
) : GenericEntity()

