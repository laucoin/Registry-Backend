package com.laucoin.registry.domain.profile.service.impl

import com.laucoin.registry.core.adapter.SecurityProperties
import com.laucoin.registry.core.model.user.EnrichedUserModel
import com.laucoin.registry.core.model.util.ErrorEnum.CANT_ALTER_LAST_EVENT_MANAGER
import com.laucoin.registry.core.model.util.ErrorEnum.CANT_ASSIGN_OR_UPDATE_HIGHER_ROLE_PROFILE
import com.laucoin.registry.core.model.util.ErrorEnum.PROFILE_CONFLICT
import com.laucoin.registry.core.model.util.PageModel
import com.laucoin.registry.core.model.util.RegistryExceptionModel
import com.laucoin.registry.core.repository.IUserModelRepository
import com.laucoin.registry.core.util.notFoundIfEmpty
import com.laucoin.registry.core.util.paginate
import com.laucoin.registry.domain.profile.model.EnrichedProfileModel
import com.laucoin.registry.domain.profile.model.ProfileModel
import com.laucoin.registry.domain.profile.model.ProfilesCreationModel
import com.laucoin.registry.domain.profile.repository.IProfileModelRepository
import com.laucoin.registry.domain.profile.service.IEventProfileService
import com.laucoin.registry.domain.profile.service.util.GenericProfileServiceImpl
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.Objects
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

@Service
class EventProfileService(
    repository: IProfileModelRepository,
    securityProperties: SecurityProperties,
    private val userRepository: IUserModelRepository,
): IEventProfileService, GenericProfileServiceImpl(repository, securityProperties) {
    override fun getPage(
        eventId: UUID,
        pageIndex: Int,
        pageSize: Int,
        order: Direction,
        onlyNonBlocked: Boolean,
        onlyAccepted: Boolean,
        searched: String?,
        startAccess: LocalDateTime?,
        endAccess: LocalDateTime?
    ): Mono<PageModel<EnrichedProfileModel>> {
        return repository.getAllByOutdatedAndEventId(
            eventId = eventId,
            outdated = false,
            accepted = onlyAccepted,
            onlyVisible = onlyNonBlocked,
        )
            .customFilter(searched, startAccess, endAccess)
            .customSort(order)
            .fillServiceAccountUser()
            .paginate(pageIndex, pageSize)
    }

    override fun findById(currentUser: EnrichedUserModel, eventId: UUID, id: UUID): Mono<EnrichedProfileModel> =
        repository.findById(id, onlyVisible = true)
            .filter { it.eventId == eventId }
            .notFoundIfEmpty(id)
            .fillServiceAccountUser()

    override fun getRoles(currentUser: EnrichedUserModel, eventId: UUID, id: UUID?): Mono<List<String?>> {
        val currentUserProfile: EnrichedProfileModel = currentUser.profiles !!.first { it.eventId == eventId }
        val roles = securityProperties.profileRoles()

        if (Objects.isNull(id)) {
            return Mono.just(getNonLastProfilePossibleRole(currentUserProfile.role !!, roles))
        }

        return findById(currentUser, eventId, id !!)
            .flatMap { profile ->
                val highestRole = roles.first()
                if (profile.role != highestRole) return@flatMap Mono.just(
                    getNonLastProfilePossibleRole(
                        currentUserProfile.role !!,
                        roles
                    )
                )
                getAllProfilesWithHighestRoleByEventIdExcludingUserId(eventId, profile.userId !!)
                    .map { profiles ->
                        if (profiles.isEmpty()) listOf(profile.role)
                        else getNonLastProfilePossibleRole(currentUserProfile.role !!, roles)
                    }
            }
    }

    private fun getNonLastProfilePossibleRole(currentUserRole: String, roles: List<String?>): List<String?> {
        val startIndex = roles.indexOf(currentUserRole)
        return roles.subList(startIndex, roles.size)
    }

    override fun createSupportProfile(currentUser: EnrichedUserModel, role: String, eventId: UUID): Mono<ProfileModel> {
        val now = now()
        val profile = ProfileModel()
        profile.let {
            it.eventId = eventId
            it.userId = currentUser.id
            it.role = role
            it.startAccess = now
            it.endAccess = now.plusDays(1)
        }
        profile.create(currentUser)
        return repository.create(profile)
    }

    override fun createMultiple(currentUser: EnrichedUserModel, eventId: UUID, profiles: ProfilesCreationModel): Flux<ProfileModel> {
        checkCurrentUserHasEnoughRole(currentUser, eventId, profiles.role !!)

        return getUsersByEmails(profiles.users)
            .collectList()
            .removeUsersWithProfileCollision(eventId, profiles)
            .toFlux()
            .createProfileFromUserAndProfiles(currentUser, eventId, profiles)
            .flatMap { repository.createAll(it) }
    }

    private fun getUsersByEmails(emails: Set<String>?): Flux<EnrichedUserModel> =
        userRepository.findByEmails(emails?.toList() ?: emptyList(), onlyVisible = true)

    private fun Mono<List<EnrichedUserModel>>.removeUsersWithProfileCollision(
        eventId: UUID,
        profiles: ProfilesCreationModel
    ): Mono<List<EnrichedUserModel>> =
        flatMap { users ->
            repository.getAllByOutdatedAndEventId(
                eventId = eventId,
                outdated = false,
                accepted = false,
                onlyVisible = true,
            )
                .filter { profile ->
                    users.any { it.id == profile.userId } && listOf(
                        profiles.startAccess,
                        profiles.endAccess
                    ).areInRange(profile.startAccess, profile.endAccess)
                }
                .collectList()
                .map { users.filter { user -> it.any { profile -> profile.userId != user.id } } }
        }

    private fun Flux<List<EnrichedUserModel>>.createProfileFromUserAndProfiles(
        currentUser: EnrichedUserModel,
        eventId: UUID,
        profiles: ProfilesCreationModel
    ): Flux<List<ProfileModel>> =
        map { users ->
            users.map { user ->
                val profile = ProfileModel()
                profile.let {
                    it.eventId = eventId
                    it.userId = user.id
                    it.role = profiles.role
                    it.startAccess = profiles.startAccess
                    it.endAccess = profiles.endAccess
                }
                profile.create(currentUser)
                profile
            }
        }

    override fun updateById(currentUser: EnrichedUserModel, eventId: UUID, id: UUID, profile: ProfileModel): Mono<ProfileModel> {
        checkCurrentUserHasEnoughRole(currentUser, eventId, profile.role !!)
        return findById(currentUser, eventId, id)
            .checkLastEventManagerBeforeUpdate(eventId, profile)
            .checkProfileCollision(eventId, profile)
            .flatMap {
                it.role = profile.role
                it.startAccess = profile.startAccess
                it.endAccess = profile.endAccess
                it.update(currentUser)
                repository.updateById(id, it)
            }
    }

    private fun Mono<EnrichedProfileModel>.checkLastEventManagerBeforeUpdate(
        eventId: UUID,
        profile: ProfileModel
    ): Mono<EnrichedProfileModel> =
        flatMap {
            val roles = securityProperties.profileRoles()
            val highestRole = roles.first()

            if (it.role == highestRole && profile.role != highestRole) {
                getAllProfilesWithHighestRoleByEventIdExcludingUserId(eventId, it.userId !!)
                    .handle { profiles, sink ->
                        if (profiles.isEmpty()) {
                            sink.error(RegistryExceptionModel(FORBIDDEN, CANT_ALTER_LAST_EVENT_MANAGER.name))
                        } else sink.next(it)
                    }
            } else Mono.just(it)
        }

    private fun Mono<EnrichedProfileModel>.checkProfileCollision(eventId: UUID, profile: ProfileModel): Mono<EnrichedProfileModel> =
        flatMap { oldProfile ->
            if (oldProfile.startAccess != profile.startAccess || oldProfile.endAccess != profile.endAccess) {
                repository.getAllByOutdatedAndUserId(
                    userId = oldProfile.userId !!,
                    outdated = true,
                    accepted = true,
                    onlyVisible = false,
                )
                    .filter {
                        it.eventId == eventId && listOf(
                            profile.startAccess,
                            profile.endAccess
                        ).areInRange(oldProfile.startAccess, oldProfile.endAccess)
                    }
                    .collectList()
                    .handle { it, handle ->
                        if (it.isNotEmpty()) handle.error(RegistryExceptionModel(FORBIDDEN, PROFILE_CONFLICT.name))
                        else handle.next(oldProfile)
                    }
            } else Mono.just(oldProfile)
        }

    private fun checkCurrentUserHasEnoughRole(currentUser: EnrichedUserModel, eventId: UUID, role: String) {
        val roles = securityProperties.profileRoles()
        val currentUserProfile = currentUser.profiles?.find { it.eventId == eventId }

        if (roles.indexOf(currentUserProfile?.role) < roles.indexOf(role)) {
            throw RegistryExceptionModel(FORBIDDEN, CANT_ASSIGN_OR_UPDATE_HIGHER_ROLE_PROFILE.name)
        }
    }

    override fun blockById(currentUser: EnrichedUserModel, eventId: UUID, id: UUID): Mono<ProfileModel> =
        findById(currentUser, eventId, id)
            .checkLastEventManager(eventId)
            .updateVisibility(currentUser, visibility = false)

    private fun Mono<EnrichedProfileModel>.checkLastEventManager(eventId: UUID): Mono<EnrichedProfileModel> =
        flatMap {
            val roles = securityProperties.profileRoles()
            val highestRole = roles.first()

            if (it.role == highestRole) {
                getAllProfilesWithHighestRoleByEventIdExcludingUserId(eventId, it.userId !!)
                    .handle { profiles, sink ->
                        if (profiles.isEmpty()) {
                            sink.error(RegistryExceptionModel(FORBIDDEN, CANT_ALTER_LAST_EVENT_MANAGER.name))
                        } else sink.next(it)
                    }
            } else Mono.just(it)
        }


    override fun unblockById(currentUser: EnrichedUserModel, eventId: UUID, id: UUID): Mono<ProfileModel> {
        return findById(currentUser, eventId, id)
            .updateVisibility(currentUser, visibility = true)
    }

    private fun Mono<EnrichedProfileModel>.updateVisibility(currentUser: EnrichedUserModel, visibility: Boolean): Mono<ProfileModel> =
        flatMap {
            it.visible = visibility
            it.update(currentUser)
            repository.updateById(it.id !!, it)
        }

    override fun deleteById(currentUser: EnrichedUserModel, eventId: UUID, id: UUID): Mono<Void> =
        findById(currentUser, eventId, id)
            .checkLastEventManager(eventId)
            .flatMap { repository.deleteById(id) }

    override fun fillServiceAccountUser(element: EnrichedProfileModel): EnrichedProfileModel {
        element.fillHistoryWithServiceAccountIfNecessary(serviceAccount)
        return element
    }
}
