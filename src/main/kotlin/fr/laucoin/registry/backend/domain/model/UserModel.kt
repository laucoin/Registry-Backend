package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.UserTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.UserTypeEnum.USER
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.ZonedDateTime.now
import java.util.UUID


open class UserModel(
	var oidcId: UUID? = null,
	var type: UserTypeEnum = USER,
	var firstName: String? = null,
	var lastName: String? = null,
	var email: String? = null,
	var role: String? = null,
	var birthday: LocalDate? = null,
	var lastLogin: ZonedDateTime? = now(),
) : GenericModel() {
	fun personalDataChanged(email: String, firstName: String?, lastName: String?): Boolean =
		this.email != email || this.firstName != firstName || this.lastName != lastName
}
