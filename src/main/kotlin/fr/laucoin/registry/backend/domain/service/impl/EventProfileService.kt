package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_ASSIGNS_ROLE_HIGHER_THAN_CURRENT_USER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_BLOCK_LAST_EVENT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_DELETE_LAST_EVENT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_UPDATE_LAST_EVENT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_UPDATE_ROLE_HIGHER_THAN_CURRENT_USER
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.EventProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.repository.IEventProfileModelRepository
import fr.laucoin.registry.backend.domain.repository.IUserModelRepository
import fr.laucoin.registry.backend.domain.service.GenericProfileService
import fr.laucoin.registry.backend.domain.service.IEventProfileService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserEventProfileService
import java.time.LocalTime
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class EventProfileService(
    private val profileService: IUserEventProfileService,
    private val repository: IEventProfileModelRepository,
    private val roleService: IRoleService,
    private val userRepository: IUserModelRepository,
    @Value("\${registry.feature.profile.searched.max-user-result}")
    private val maxUserResult: Int,
): IEventProfileService, GenericProfileService(repository) {
    override fun findEventProfilesPage(
        eventId: UUID,
        pageable: PageableModel,
        searchParams: EventProfileSearchParamModel,
    ): Mono<PageModel<EventProfileModel>> {
        return repository
            .findEventProfilesPageByEventId(eventId, pageable, searchParams)
    }

    override fun findEventProfileById(eventId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<EventProfileModel> {
        return repository.findById(eventId, id, visibilitySearched)
            .notFoundIfEmpty(id)
    }

    override fun searchUsers(textSearched: String?): Flux<UserModel> {
        return userRepository.findWithLimit(maxUserResult, UserSearchParamModel(textSearched, visibilitySearched = true))
    }

    override fun getAssignableEventRoles(currentUser: CurrentUserModel, eventId: UUID): Flux<String> {
        return repository.findEventProfileByEventAndUserId(
            eventId,
            currentUser.id !!,
            EventProfileSearchParamModel(
                visibilitySearched = true,
                availabilitySearched = true,
                statusSearched = listOf(ACCEPTED),
            ),
        )
            .notFoundIfEmpty(Pair(eventId, currentUser.id !!))
            .map { roleService.getAssignableEventRoles(it) }
            .flatMapMany { Flux.fromIterable(it) }
    }

    override fun createEventProfiles(
        currentUser: CurrentUserModel,
        eventId: UUID,
        userIds: List<UUID>,
        profiles: List<EventProfileModel>
    ): Mono<Pair<List<UUID>, List<UUID>>> {
        return validateNoProfileConflict(
            eventId,
            userIds,
            profileId = null,
            profiles.first().startAccess?.toLocalDateTime(LocalTime.MIN),
            profiles.first().endAccess?.toLocalDateTime(LocalTime.MAX)
        )
            .map { allowedUsers ->
                profiles.filter { allowedUsers.contains(it.user !!.id) }
                    .map { it.apply { create(currentUser) } }
            }
            .flatMapMany { repository.saveAll(it) }
            .collectList()
            .map {
                val savedUserId = it.mapNotNull { profile -> profile.user !!.id }
                Pair(savedUserId, userIds.minus(savedUserId.toSet()))
            }
    }

    override fun updateEventProfileById(
        currentUser: CurrentUserModel,
        eventId: UUID,
        id: UUID,
        profile: EventProfileModel
    ): Mono<EventProfileModel> {
        return findEventProfileById(eventId, id, visibilitySearched = null)
            .flatMap {
                validateNoProfileConflict(
                    eventId,
                    listOf(it.user !!.id !!),
                    it.id,
                    profile.endAccess?.toLocalDateTime(LocalTime.MIN),
                    profile.endAccess?.toLocalDateTime(LocalTime.MAX),
                ).map { _ -> it }
            }
            .validateRole(currentUser, eventId, profile)
            .validateNotLastEventRoleLevel0(EVENT_PROFILE_UPDATE_LAST_EVENT_ADMINISTRATOR)
            .map {
                it.apply {
                    role = profile.role
                    startAccess = profile.startAccess
                    endAccess = profile.endAccess
                }
            }
            .updateEventProfile(currentUser)
    }

    override fun blockEventProfileById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<EventProfileModel> {
        return findEventProfileById(eventId, id, visibilitySearched = true)
            .validateNotLastEventRoleLevel0(EVENT_PROFILE_BLOCK_LAST_EVENT_ADMINISTRATOR)
            .updateVisibility(visibility = false)
            .updateEventProfile(currentUser)
    }

    override fun unblockEventProfileById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<EventProfileModel> {
        return findEventProfileById(eventId, id, visibilitySearched = false)
            .updateVisibility(visibility = true)
            .updateEventProfile(currentUser)
    }

    override fun deleteEventProfileById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<Void> {
        return findEventProfileById(eventId, id, visibilitySearched = null)
            .validateNotLastEventRoleLevel0(EVENT_PROFILE_DELETE_LAST_EVENT_ADMINISTRATOR)
            .flatMap { repository.deleteById(id) }
    }

    private fun Mono<EventProfileModel>.validateNotLastEventRoleLevel0(error: String) = flatMap {
        profileService.validateNotLastEventRoleLevel0(it.user !!.id !!, it.event !!.id !!, it, error)
    }

    private fun Mono<EventProfileModel>.validateRole(
        currentUser: CurrentUserModel, eventId: UUID, profile: EventProfileModel
    ) = flatMap { profileToUpdate ->
        repository.findEventProfileByEventAndUserId(
            eventId,
            currentUser.id !!,
            EventProfileSearchParamModel(
                visibilitySearched = true,
                availabilitySearched = true,
                statusSearched = listOf(ACCEPTED),
            )
        )
            .handle { it, handle ->
                val eligibleRoles = roleService.getAssignableEventRoles(it)
                if (! eligibleRoles.contains(profileToUpdate.role)) {
                    log.warn(
                        "User \"{}\" cannot update event profile with a role higher up the breast.",
                        currentUser.id,
                    )
                    handle.error(RegistryException(FORBIDDEN, EVENT_PROFILE_UPDATE_ROLE_HIGHER_THAN_CURRENT_USER))
                } else if (! eligibleRoles.contains(profile.role)) {
                    log.warn(
                        "User \"{}\" tried to update event profile with a role higher up the breast.",
                        currentUser.id,
                    )
                    handle.error(RegistryException(FORBIDDEN, EVENT_PROFILE_ASSIGNS_ROLE_HIGHER_THAN_CURRENT_USER))
                } else handle.next(profileToUpdate)
            }
    }

    private fun Mono<EventProfileModel>.updateEventProfile(currentUser: CurrentUserModel) = flatMap {
        repository.update(it.apply { update(currentUser) })
    }
}
