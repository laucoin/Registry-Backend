package com.laucoin.registry.core.service.impl

import com.laucoin.registry.core.model.user.UserModel
import com.laucoin.registry.core.model.util.ErrorEnum.DISABLED_ACCOUNT
import com.laucoin.registry.core.model.util.ErrorEnum.FAILED_TO_GET_ID_OR_EMAIL_AT_LOGIN
import com.laucoin.registry.core.model.util.RegistryExceptionModel
import com.laucoin.registry.core.repository.IUserRepository
import com.laucoin.registry.core.service.IUserService
import com.laucoin.registry.core.util.Logger
import com.laucoin.registry.core.util.getClaimAsUUID
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@Service
class UserService(
    @Value("\${registry.security.sso.claim-keys.user-id}")
    private val userIdKey: String,
    @Value("\${registry.security.sso.claim-keys.email}")
    private val emailKey: String,
    @Value("\${registry.security.sso.claim-keys.first-name:}")
    private val firstNameKey: String?,
    @Value("\${registry.security.sso.claim-keys.last-name:}")
    private val lastNameKey: String?,
    private val repository: IUserRepository,
): IUserService, Logger() {
    override fun signIn(serviceAccount: UserModel, decodedToken: Jwt, lestUserRole: String): Mono<UserModel> {
        if (! decodedToken.hasClaim(userIdKey) || ! decodedToken.hasClaim(emailKey)) {
            return Mono.error(RegistryExceptionModel(UNAUTHORIZED, FAILED_TO_GET_ID_OR_EMAIL_AT_LOGIN.name))
        }

        val oidcId: UUID = decodedToken.getClaimAsUUID(userIdKey) !!
        val email: String = decodedToken.getClaimAsString(emailKey) !!
        val firstName: String? = decodedToken.getClaimAsString(firstNameKey)
        val lastName: String? = decodedToken.getClaimAsString(lastNameKey)

        return repository.findByOidcId(oidcId, onlyVisible = false)
            .createNewUser(serviceAccount, oidcId, lestUserRole, email, firstName, lastName)
            .handle { it, sink ->
                if (it.visible) {
                    sink.next(it)
                } else {
                    log.warn("Signing in attempt blocked for user \"{}\" due to disabled account", it.id)
                    sink.error(RegistryExceptionModel(UNAUTHORIZED, DISABLED_ACCOUNT.name))
                }
            }
            .updateUserIfNecessary(serviceAccount, email, firstName, lastName)
    }

    private fun Mono<UserModel>.createNewUser(
        serviceAccount: UserModel,
        oidcId: UUID,
        role: String,
        email: String,
        firstName: String?,
        lastName: String?
    ): Mono<UserModel> = switchIfEmpty {
        log.info("Saving new user (OIDC ID \"{}\") in database", oidcId)
        val user = UserModel(
            oidcId = oidcId,
            role = role,
            email = email,
            firstName = firstName,
            lastName = lastName,
        )
        user.create(serviceAccount)
        repository.save(user).flatMap { repository.findById(it.id !!, onlyVisible = false) }
    }

    private fun Mono<UserModel>.updateUserIfNecessary(
        serviceAccount: UserModel,
        email: String,
        firstName: String?,
        lastName: String?
    ): Mono<UserModel> = flatMap { user ->
        val personalDataChanged = user.personalDataChanged(email, firstName, lastName)
        val defaultProfileDisable = user.defaultProfileDisable()

        if (! personalDataChanged && ! defaultProfileDisable) {
            return@flatMap Mono.just(user)
        }

        if (personalDataChanged) {
            log.info("Updating personal data for user \"{}\"", user.id)
            user.email = email
            user.firstName = firstName
            user.lastName = lastName
        }

        if (defaultProfileDisable) {
            log.info(
                "Removing user \"{}\" default profile because, the profile \"{}\" is disabled itself",
                user.id,
                user.defaultProfileId
            )
            user.defaultProfileId = null
        }

        log.info("Saving user \"{}\" update", user.id)
        user.update(serviceAccount)
        repository.save(user).flatMap { repository.findById(it.id !!, onlyVisible = false) }
    }
}
