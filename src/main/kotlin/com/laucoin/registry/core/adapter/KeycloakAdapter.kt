package com.laucoin.registry.core.adapter

import com.laucoin.registry.core.model.user.EnrichedUserModel
import com.laucoin.registry.core.model.util.ErrorEnum.OIDC_LOGIN_FAILED
import com.laucoin.registry.core.model.util.RegistryExceptionModel
import com.laucoin.registry.core.service.IAuthService
import com.laucoin.registry.core.util.Logger
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import reactor.core.publisher.Mono

class KeycloakAdapter(
    private val decoder: ReactiveJwtDecoder,
    private val service: IAuthService,
    private val securityProperties: SecurityProperties,
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
        log.warn("Signing in attempt failed \"{}\"", it.message)
        Mono.error(RegistryExceptionModel(UNAUTHORIZED, OIDC_LOGIN_FAILED.name, it))
    }

    private fun Mono<Jwt>.fetchUser(): Mono<EnrichedUserModel> =
        flatMap { service.fetchUser(securityProperties.serviceAccount(), it) }

    private fun Mono<EnrichedUserModel>.extractAuthorities(): Mono<Pair<EnrichedUserModel, Collection<GrantedAuthority>>> =
        map { user ->
            val authorities = ArrayList<GrantedAuthority>()
            authorities.add(SimpleGrantedAuthority("ROLE_REGISTRY_USER_${user.id}"))

            user.authorities = securityProperties.userAuthorities(user.role)
            user.authorities?.forEach {
                authorities.add(GrantedAuthority { it.name })
            }

            user.profiles = user.profiles?.map { profile ->
                profile.authorities = securityProperties.eventAuthorities(profile.role).map {
                    authorities.add(GrantedAuthority { it.name })
                    it
                }
                profile
            }

            Pair(user, authorities)
        }

    private fun Mono<Pair<EnrichedUserModel, Collection<GrantedAuthority>>>.signIn(token: String): Mono<Authentication> =
        map { (user, authorities) ->
            log.debug("Finishing user \"{}\" sign in with following authorities: {}", user.id, authorities)
            UsernamePasswordAuthenticationToken(user.toString(), token, authorities)
        }
}
