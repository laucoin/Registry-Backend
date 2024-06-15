package com.laucoin.registry.core.service

import com.laucoin.registry.core.model.user.UserModel
import org.springframework.security.oauth2.jwt.Jwt
import reactor.core.publisher.Mono

interface IUserService {
    fun signIn(serviceAccount: UserModel, decodedToken: Jwt, lestUserRole: String): Mono<UserModel>
}
