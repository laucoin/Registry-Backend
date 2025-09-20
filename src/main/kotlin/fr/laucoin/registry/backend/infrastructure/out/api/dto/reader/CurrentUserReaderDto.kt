package fr.laucoin.registry.backend.infrastructure.out.api.dto.reader

import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import java.time.LocalDate
import java.time.ZonedDateTime

data class CurrentUserReaderDto(
	var authorities: List<String>,
	var preferences: PreferenceReaderDto? = null,
	var firstName: String? = null,
	var lastName: String? = null,
	var email: String? = null,
	var role: LabelDto? = null,
	var birthday: LocalDate? = null,
	var lastLogin: ZonedDateTime? = null,
	var purged: Boolean,
): GenericReaderDto()
