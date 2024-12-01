package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_ALREADY_EXIST_ON_RANGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_ASSIGNS_ROLE_HIGHER_THAN_CURRENT_USER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_DELETE_LAST_EVENT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_UPDATE_LAST_EVENT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_UPDATE_ROLE_HIGHER_THAN_CURRENT_USER
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.REJECTED
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.RegistryExceptionModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.repository.IEventProfileModelRepository
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IEventProfileService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserEventProfileService
import java.time.ZonedDateTime
import java.time.ZonedDateTime.now
import java.util.Objects
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class EventProfileService(
    private val repository: IEventProfileModelRepository,
    private val profileService: IUserEventProfileService,
    private val roleService: IRoleService
): IEventProfileService, GenericService<EventProfileModel>(compareBy { it.user?.lastName }) {
    override fun findEventProfilesByEventId(
        eventId: UUID,
        order: Direction,
        onlyVisible: Boolean,
        status: ProfileStatusEnum?,
        searched: String?,
        startAccess: ZonedDateTime?,
        endAccess: ZonedDateTime?
    ): Flux<EventProfileModel> {
        return repository.findEventProfilesByEventId(eventId, onlyVisible, onlyUsable = false, status, startAccess, endAccess)
            .searchAndSort(order, searched)
    }

    override fun findEventProfileByEventIdAndId(eventId: UUID, id: UUID, onlyVisible: Boolean): Mono<EventProfileModel> {
        return repository.findById(eventId, id, onlyVisible)
            .notFoundIfEmpty(id)
    }

    override fun getAssignableEventRoles(currentUser: UserModel, eventId: UUID): Mono<List<String>> {
        return repository.findEventProfileByEventAndUserId(
            eventId,
            currentUser.id !!,
            onlyVisible = true,
            onlyUsable = true,
            status = ACCEPTED,
        )
            .notFoundIfEmpty(Pair(eventId, currentUser.id !!))
            .map { roleService.getAssignableEventRoles(it) }
    }

    override fun createSupportEventProfile(currentUser: UserModel, eventId: UUID): Mono<EventProfileModel> {
        val profile = EventProfileModel().apply {
            user = currentUser
            event = EventModel().apply { id = eventId }
            role = roleService.getLevel0RoleFromEventRoles()
            status = ACCEPTED
            startAccess = now()
            endAccess = now().plusHours(1)
            create(currentUser)
        }

        return validateNoProfileConflict(eventId, listOf(currentUser.id !!), profileId = null, profile.startAccess, profile.endAccess)
            .flatMap { repository.save(profile) }
    }

    override fun createEventProfiles(
        currentUser: UserModel,
        eventId: UUID,
        userIds: List<UUID>,
        profiles: List<EventProfileModel>
    ): Flux<EventProfileModel> {
        return validateNoProfileConflict(eventId, userIds, profileId = null, profiles.first().startAccess, profiles.first().endAccess)
            .map { allowedUsers ->
                profiles.filter { allowedUsers.contains(it.user?.id) }
                    .map { it.apply { create(currentUser) } }
            }
            .flatMapMany { repository.saveAll(it) }
    }

    override fun updateEventProfileById(
        currentUser: UserModel,
        eventId: UUID,
        id: UUID,
        profile: EventProfileModel
    ): Mono<EventProfileModel> {
        return findEventProfileByEventIdAndId(eventId, id, onlyVisible = false)
            .flatMap {
                validateNoProfileConflict(eventId, listOf(it.user?.id !!), it.id, profile.endAccess, profile.endAccess)
                    .map { _ -> it }
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

    override fun blockEventProfileById(currentUser: UserModel, eventId: UUID, id: UUID): Mono<EventProfileModel> {
        return findEventProfileByEventIdAndId(eventId, id, onlyVisible = true)
            .validateNotLastEventRoleLevel0(EVENT_PROFILE_DELETE_LAST_EVENT_ADMINISTRATOR)
            .updateVisibility(visibility = false)
            .updateEventProfile(currentUser)
    }

    override fun unblockEventProfileById(currentUser: UserModel, eventId: UUID, id: UUID): Mono<EventProfileModel> {
        return findEventProfileByEventIdAndId(eventId, id, onlyVisible = false)
            .updateVisibility(visibility = true)
            .updateEventProfile(currentUser)
    }

    override fun deleteEventProfileById(currentUser: UserModel, eventId: UUID, id: UUID): Mono<Void> {
        return findEventProfileByEventIdAndId(eventId, id, onlyVisible = false)
            .validateNotLastEventRoleLevel0(EVENT_PROFILE_DELETE_LAST_EVENT_ADMINISTRATOR)
            .flatMap { repository.deleteById(it.id !!) }
    }

    private fun Mono<EventProfileModel>.validateNotLastEventRoleLevel0(error: String) = flatMap {
        profileService.validateNotLastEventRoleLevel0(it.user?.id !!, it.event?.id !!, it, error)
    }

    private fun Mono<EventProfileModel>.validateRole(
        currentUser: UserModel, eventId: UUID, profile: EventProfileModel
    ) = flatMap { profileToUpdate ->
        repository.findEventProfileByEventAndUserId(
            eventId,
            currentUser.id !!,
            onlyVisible = true,
            onlyUsable = true,
            status = ACCEPTED,
        )
            .handle { it, handle ->
                val eligibleRoles = roleService.getAssignableEventRoles(it)
                if (! eligibleRoles.contains(profileToUpdate.role)) {
                    log.warn(
                        "User \"{}\" cannot update event profile with a role higher up the breast.",
                        currentUser.id,
                    )
                    handle.error(RegistryExceptionModel(FORBIDDEN, EVENT_PROFILE_UPDATE_ROLE_HIGHER_THAN_CURRENT_USER))
                } else if (! eligibleRoles.contains(profile.role)) {
                    log.warn(
                        "User \"{}\" tried to update event profile with a role higher up the breast.",
                        currentUser.id,
                    )
                    handle.error(RegistryExceptionModel(FORBIDDEN, EVENT_PROFILE_ASSIGNS_ROLE_HIGHER_THAN_CURRENT_USER))
                } else handle.next(profileToUpdate)
            }
    }

    private fun Mono<EventProfileModel>.updateEventProfile(currentUser: UserModel) = flatMap {
        repository.save(it.apply { update(currentUser) })
    }

    private fun validateNoProfileConflict(
        eventId: UUID,
        userIds: List<UUID>,
        profileId: UUID?,
        startAccess: ZonedDateTime?,
        endAccess: ZonedDateTime?
    ): Mono<List<UUID>> {
        return repository.findEventProfilesByEventId(
            eventId,
            onlyVisible = false,
            onlyUsable = false,
            status = null,
            startAccess,
            endAccess
        )
            .filter { it.status !== REJECTED }
            .filter { userIds.contains(it.user?.id) && (Objects.isNull(profileId) || it.id != profileId) }
            .collectList()
            .handle { profiles, handle ->
                when {
                    profiles.size == userIds.size -> {
                        log.warn("Another profile already exist for the user(s) \"{}\" on the event \"{}\".", userIds, eventId)
                        handle.error(
                            RegistryExceptionModel(
                                CONFLICT,
                                EVENT_PROFILE_ALREADY_EXIST_ON_RANGE
                            )
                        )
                    }

                    profiles.isNotEmpty() -> {
                        log.warn(
                            "Partial request because, profile already exist for the user(s) \"{}\" on the event \"{}\".",
                            userIds.filter { Objects.isNull(profiles.find { profile -> profile.user?.id != it }) },
                            eventId
                        )
                        handle.next(userIds.filter { Objects.isNull(profiles.find { profile -> profile.user?.id == it }) })
                    }

                    else -> handle.next(userIds)
                }
            }
    }
}
