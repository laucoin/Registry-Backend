package com.laucoin.registry.core.service

import com.laucoin.registry.core.model.user.EnrichedUserModel
import org.springframework.security.oauth2.jwt.Jwt
import reactor.core.publisher.Mono

interface IAuthService {
    fun fetchUser(serviceAccount: EnrichedUserModel, decodedToken: Jwt): Mono<EnrichedUserModel>
}
