package com.laucoin.registry.core.adapter

import com.laucoin.registry.core.model.user.UserModel
import com.laucoin.registry.core.model.util.ErrorEnum.OIDC_LOGIN_FAILED
import com.laucoin.registry.core.model.util.RegistryExceptionModel
import com.laucoin.registry.core.service.IUserService
import com.laucoin.registry.core.util.Logger
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import reactor.core.publisher.Mono

class KeycloakAdapter(
    private val decoder: ReactiveJwtDecoder,
    private val userService: IUserService,
    private val appManagementProperties: AppManagementProperties,
): ReactiveAuthenticationManager, Logger() {
    override fun authenticate(authentication: Authentication): Mono<Authentication> {
        val token: String = authentication.credentials.toString()
        return decoder.decode(token)
            .onLoginFailed()
            .fetchUser()
            .extractAuthorities()
            .signIn(token)
    }

    private fun Mono<Jwt>.onLoginFailed(): Mono<Jwt> = onErrorResume {
        log.warn("Signing in attempt failed due to: {}", it.message)
        Mono.error(RegistryExceptionModel(UNAUTHORIZED, OIDC_LOGIN_FAILED.name, it))
    }

    private fun Mono<Jwt>.fetchUser(): Mono<UserModel> =
        flatMap { userService.signIn(appManagementProperties.serviceAccount(), it, appManagementProperties.getLeastUserRole()) }

    private fun Mono<UserModel>.extractAuthorities(): Mono<Pair<UserModel, Collection<GrantedAuthority>>> =
        map { user -> Pair(user, appManagementProperties.getAuthorities(user)) }

    private fun Mono<Pair<UserModel, Collection<GrantedAuthority>>>.signIn(token: String): Mono<Authentication> =
        map { (user, authorities) ->
            log.debug("Finishing user \"{}\" sign in with following authorities: {}", user.id, authorities)
            UsernamePasswordAuthenticationToken(user.toString(), token, authorities)
        }
}
