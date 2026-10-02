package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

import com.fasterxml.jackson.annotation.JsonProperty
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.ZonedDateTime.now

data class UserReaderDto(
	var firstName: String? = null,
	var lastName: String? = null,
	var email: String? = null,
	var role: LabelDto? = null,
	var birthday: LocalDate? = null,
	var lastLogin: ZonedDateTime? = now(),
	@get:JsonProperty("purged")
	@param:JsonProperty("purged")
	var isPurged: Boolean = false,
): GenericReaderDto()
