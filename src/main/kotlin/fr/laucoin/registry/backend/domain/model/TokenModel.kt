package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime

data class TokenModel(
    val type: String = "Bearer",
    var token: String? = null,
    var issuedAt: ZonedDateTime? = null,
    var expiredAt: ZonedDateTime? = null,
)
