package com.laucoin.registry.domain.profile.service.impl

import com.laucoin.registry.core.adapter.SecurityProperties
import com.laucoin.registry.core.model.user.EnrichedUserModel
import com.laucoin.registry.core.model.util.ErrorEnum.CANT_ALTER_LAST_EVENT_MANAGER
import com.laucoin.registry.core.model.util.PageModel
import com.laucoin.registry.core.model.util.RegistryExceptionModel
import com.laucoin.registry.core.util.notFoundIfEmpty
import com.laucoin.registry.core.util.paginate
import com.laucoin.registry.domain.profile.model.EnrichedProfileModel
import com.laucoin.registry.domain.profile.model.ProfileModel
import com.laucoin.registry.domain.profile.repository.IProfileModelRepository
import com.laucoin.registry.domain.profile.service.IUserProfileService
import com.laucoin.registry.domain.profile.service.util.GenericProfileServiceImpl
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class UserProfileService(
    repository: IProfileModelRepository,
    securityProperties: SecurityProperties,
): IUserProfileService, GenericProfileServiceImpl(repository, securityProperties) {
    override fun getPage(
        userId: UUID,
        pageIndex: Int,
        pageSize: Int,
        order: Direction,
        onlyAccepted: Boolean,
        searched: String?,
        startAccess: LocalDateTime?,
        endAccess: LocalDateTime?
    ): Mono<PageModel<EnrichedProfileModel>> {
        return repository.getAllByOutdatedAndUserId(
            userId = userId,
            outdated = false,
            accepted = onlyAccepted,
            onlyVisible = true,
        )
            .customFilter(searched, startAccess, endAccess)
            .customSort(order)
            .fillServiceAccountUser()
            .paginate(pageIndex, pageSize)
    }

    override fun findById(currentUser: EnrichedUserModel, userId: UUID, id: UUID): Mono<EnrichedProfileModel> =
        repository.findById(id, onlyVisible = true)
            .filter { it.userId == userId }
            .notFoundIfEmpty(id)
            .fillServiceAccountUser()

    override fun manageProfileAcceptance(
        currentUser: EnrichedUserModel,
        userId: UUID,
        id: UUID,
        accepted: Boolean
    ): Mono<ProfileModel> {
        return findById(currentUser, userId, id)
            .flatMap { profile ->
                if (accepted) {
                    profile.accepted = true
                    profile.update(currentUser)
                    repository.updateById(profile.id !!, profile)
                } else {
                    repository.deleteById(profile.id !!)
                        .then(Mono.fromCallable { profile })
                }
            }
    }

    override fun deleteById(currentUser: EnrichedUserModel, userId: UUID, id: UUID): Mono<Void> {
        val roles = securityProperties.profileRoles()
        val highestRole = roles.first()
        return findById(currentUser, userId, id)
            .flatMap { profile ->
                if (profile.role != highestRole || ! profile.accepted) {
                    return@flatMap repository.deleteById(id)
                }

                this.getAllProfilesWithHighestRoleByEventIdExcludingUserId(profile.eventId !!, userId)
                    .handle { it, handle ->
                        if (it.isEmpty()) {
                            log.warn(
                                "User \"{}\" attempt to delete last event \"{}\" manager profile \"{}\"",
                                currentUser.id,
                                profile.eventId,
                                profile.id,
                            )
                            handle.error(RegistryExceptionModel(FORBIDDEN, CANT_ALTER_LAST_EVENT_MANAGER.name))
                        } else handle.next(profile)
                    }
                    .flatMap { repository.deleteById(it.id !!) }
            }
    }
}
