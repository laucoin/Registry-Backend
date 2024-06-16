package com.laucoin.registry.core.util

import com.laucoin.registry.core.config.GsonConfig
import com.laucoin.registry.core.model.user.EnrichedUserModel
import java.security.Principal
import java.util.UUID
import org.springframework.security.oauth2.jwt.Jwt

fun Jwt.getClaimAsUUID(claim: String): UUID? = if (! hasClaim(claim)) null else UUID.fromString(getClaimAsString(claim))
fun Principal.currentUser(): EnrichedUserModel = GsonConfig().gson().fromJson(this.name, EnrichedUserModel::class.java)
