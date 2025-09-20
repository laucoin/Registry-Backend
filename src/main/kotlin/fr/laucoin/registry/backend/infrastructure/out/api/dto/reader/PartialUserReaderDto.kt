package fr.laucoin.registry.backend.infrastructure.out.api.dto.reader

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import java.util.UUID

@JsonInclude(NON_NULL)
data class PartialUserReaderDto(
	var id: UUID? = null,
	var firstName: String? = null,
	var lastName: String? = null,
	var email: String? = null,
)
