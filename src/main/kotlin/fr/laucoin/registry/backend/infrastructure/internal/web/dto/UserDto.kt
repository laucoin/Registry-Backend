package fr.laucoin.registry.backend.infrastructure.internal.web.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY
import java.util.UUID

@JsonInclude(NON_NULL)
data class UserDto(
    @JsonProperty(access = READ_ONLY)
    var id: UUID? = null,
    @JsonProperty(access = READ_ONLY)
    var firstName: String? = null,
    @JsonProperty(access = READ_ONLY)
    var lastName: String? = null,
    @JsonProperty(access = READ_ONLY)
    var email: String? = null,
)
