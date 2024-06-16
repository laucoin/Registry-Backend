package com.laucoin.registry.core.service.impl

import com.laucoin.registry.core.model.user.EnrichedUserModel
import com.laucoin.registry.core.model.user.UserModel
import com.laucoin.registry.core.model.util.ErrorEnum.DISABLED_ACCOUNT
import com.laucoin.registry.core.model.util.ErrorEnum.FAILED_TO_GET_ID_OR_EMAIL_AT_LOGIN
import com.laucoin.registry.core.model.util.RegistryExceptionModel
import com.laucoin.registry.core.repository.IUserModelRepository
import com.laucoin.registry.core.service.IAuthService
import com.laucoin.registry.core.util.Logger
import com.laucoin.registry.core.util.getClaimAsUUID
import com.laucoin.registry.domain.profile.repository.IProfileModelRepository
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@Service
class AuthService(
    @Value("\${registry.security.sso.claim-keys.user-id}")
    private val userIdKey: String,
    @Value("\${registry.security.sso.claim-keys.email}")
    private val emailKey: String,
    @Value("\${registry.security.sso.claim-keys.first-name:}")
    private val firstNameKey: String?,
    @Value("\${registry.security.sso.claim-keys.last-name:}")
    private val lastNameKey: String?,
    private val repository: IUserModelRepository,
    private val profileRepository: IProfileModelRepository,
): IAuthService, Logger() {
    override fun fetchUser(serviceAccount: EnrichedUserModel, decodedToken: Jwt): Mono<EnrichedUserModel> {
        if (! decodedToken.hasClaim(userIdKey) || ! decodedToken.hasClaim(emailKey)) {
            return Mono.error(RegistryExceptionModel(UNAUTHORIZED, FAILED_TO_GET_ID_OR_EMAIL_AT_LOGIN.name))
        }

        val oidcId: UUID = decodedToken.getClaimAsUUID(userIdKey) !!
        val email: String = decodedToken.getClaimAsString(emailKey) !!
        val firstName: String? = decodedToken.getClaimAsString(firstNameKey)
        val lastName: String? = decodedToken.getClaimAsString(lastNameKey)

        return repository.findByOidcId(oidcId, onlyVisible = false)
            .createNewUser(serviceAccount, oidcId, email, firstName, lastName)
            .handle { it, handle ->
                if (! it.visible) {
                    log.warn("Signing in attempt blocked for user \"{}\" due to disabled account", it.id)
                    handle.error(RegistryExceptionModel(UNAUTHORIZED, DISABLED_ACCOUNT.name))
                }

                handle.next(it)
            }
            .updateUserIfNecessary(serviceAccount, email, firstName, lastName)
            .addProfiles()
    }

    private fun Mono<EnrichedUserModel>.createNewUser(
        serviceAccount: EnrichedUserModel,
        oidcId: UUID,
        email: String,
        firstName: String?,
        lastName: String?
    ): Mono<EnrichedUserModel> = switchIfEmpty {
        log.info("Saving new user (OIDC ID \"{}\") in database", oidcId)
        val user = UserModel(
            oidcId = oidcId,
            email = email,
            firstName = firstName,
            lastName = lastName,
        )
        user.create(serviceAccount)
        repository.create(user).flatMap { repository.findById(it.id !!, onlyVisible = false) }
    }

    private fun Mono<EnrichedUserModel>.updateUserIfNecessary(
        serviceAccount: EnrichedUserModel,
        email: String,
        firstName: String?,
        lastName: String?
    ): Mono<EnrichedUserModel> = flatMap { user ->
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

        user.update(serviceAccount)
        repository.updateById(user.id !!, user).map { user }
    }

    private fun Mono<EnrichedUserModel>.addProfiles(): Mono<EnrichedUserModel> = flatMap { user ->
        profileRepository.getAllByActiveAndUserId(user.id !!, active = true, accepted = true, onlyVisible = false)
            .collectList()
            .map {
                user.profiles = it
                user
            }
    }
}
