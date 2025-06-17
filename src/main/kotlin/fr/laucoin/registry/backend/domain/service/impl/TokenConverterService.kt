package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTH_BLOCKED_ACCOUNT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTH_EMAIL_ALREADY_USED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTH_EMAIL_OR_ID_NOT_FOUND_IN_TOKEN
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTH_IMPERSONATED_ACCOUNT
import fr.laucoin.registry.backend.domain.extension.UserExt.getClaimAsUUID
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.JwtConversionException
import fr.laucoin.registry.backend.domain.repository.IProjectProfileModelRepository
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserService
import java.util.Objects
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.LOCKED
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty


@Component
class TokenConverterService(
    private val userService: IUserService,
    private val profileRepository: IProjectProfileModelRepository,
    private val roleService: IRoleService,
    @Value("\${registry.security.oauth2.claims.user-id}")
    private val userIdKey: String,
    @Value("\${registry.security.oauth2.claims.email}")
    private val emailKey: String,
    @Value("\${registry.security.oauth2.claims.first-name}")
    private val firstNameKey: String,
    @Value("\${registry.security.oauth2.claims.last-name}")
    private val lastNameKey: String,
): Converter<Jwt, Mono<AbstractAuthenticationToken>>, LoggerService() {

    override fun convert(jwt: Jwt): Mono<AbstractAuthenticationToken> {
        if (! jwt.hasClaim(userIdKey) || ! jwt.hasClaim(emailKey)) {
            log.error("The \"{}\" and \"{}\" keys are not found in the token", userIdKey, emailKey)
            return Mono.error(JwtConversionException(UNAUTHORIZED, AUTH_EMAIL_OR_ID_NOT_FOUND_IN_TOKEN))
        }

        val oidcId: UUID = jwt.getClaimAsUUID(userIdKey) !!
        val email: String = jwt.getClaimAsString(emailKey) !!
        val firstName: String? = jwt.getClaimAsString(firstNameKey)
        val lastName: String? = jwt.getClaimAsString(lastNameKey)

        return Mono.justOrEmpty(jwt)
            .fetchUser(oidcId)
            .throwOnBlockedUser()
            .updateUserIfPersonalDataChanged(email, firstName, lastName)
            .createNewUserOnNotFound(oidcId, email, firstName, lastName)
            .buildAuthorities()
            .map { UsernamePasswordAuthenticationToken(it, null, it.authorities) }
    }

    private fun Mono<Jwt>.fetchUser(oidcId: UUID): Mono<CurrentUserModel> =
        flatMap {
            userService.findUserByOidcId(oidcId, visibilitySearched = null)
        }

    private fun Mono<CurrentUserModel>.throwOnBlockedUser(): Mono<CurrentUserModel> = handle { it, handle ->
        if (it.isNotVisible()) {
            log.warn("Signing in attempt blocked for user \"{}\" due to disabled account", it.id)
            handle.error(JwtConversionException(LOCKED, AUTH_BLOCKED_ACCOUNT))
        } else if (it.isPurged()) {
            log.warn("Signing in attempt blocked for impersonate user \"{}\"", it.id)
            handle.error(JwtConversionException(CONFLICT, AUTH_IMPERSONATED_ACCOUNT))
        } else handle.next(it)
    }

    private fun Mono<CurrentUserModel>.updateUserIfPersonalDataChanged(
        email: String, firstName: String?, lastName: String?
    ): Mono<CurrentUserModel> = flatMap { user ->
        userService.updateUserIfPersonalDataChanged(user, email, firstName, lastName)
            .map { user }
    }

    private fun Mono<CurrentUserModel>.createNewUserOnNotFound(
        oidcId: UUID, email: String, firstName: String?, lastName: String?
    ): Mono<CurrentUserModel> = switchIfEmpty {
        log.info("User with OIDC ID \"{}\" not found, checking if an account exist with the same email", oidcId)
        if (Objects.isNull(email)) userService.createUser(oidcId, email, firstName, lastName)
        else {
            userService.findUserByEmail(email, visibilitySearched = null)
                .collectList()
                .handle { it, handle ->
                    if (it.isEmpty()) {
                        log.info("No user found with email \"{}\", creating a new user with OIDC ID \"{}\"", email, oidcId)
                        handle.next(it)
                    } else {
                        log.warn("Multiple users found with email \"{}\", cannot create a new user with OIDC ID \"{}\"", email, oidcId)
                        handle.error(JwtConversionException(CONFLICT, AUTH_EMAIL_ALREADY_USED))
                    }
                }
                .flatMap { userService.createUser(oidcId, email, firstName, lastName) }
        }
    }

    private fun Mono<CurrentUserModel>.buildAuthorities(): Mono<CurrentUserModel> = flatMap { it ->
        it.promote(roleService.getAuthoritiesByUserRole(it.role))
        profileRepository.findProjectProfilesRolesByUserId(it.id !!)
            .collectList()
            .map { profiles ->
                profiles.forEach { profile ->
                    it.promote(roleService.getAuthoritiesByProjectRole(profile.role !!, profile.projectId !!, profile.projectVisible))
                    if (profile.projectVisible == true) {
                        it.promote(
                            roleService.getOptionAuthoritiesByProject(
                                profile.projectId !!,
                                projectOptions = profile.projectOptions ?: emptyList()
                            )
                        )
                    }
                }
                it
            }
    }
}
