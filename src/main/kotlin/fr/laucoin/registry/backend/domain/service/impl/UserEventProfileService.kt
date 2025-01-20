package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_DELETE_LAST_EVENT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.EventProfileRoleCountModel
import fr.laucoin.registry.backend.domain.model.GenericModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.repository.IEventProfileModelRepository
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserEventProfileService
import java.time.ZonedDateTime
import java.util.Objects
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class UserEventProfileService(
    private val repository: IEventProfileModelRepository,
    private val roleService: IRoleService,
): IUserEventProfileService, GenericService() {
    override fun findUserEventProfiles(
        userId: UUID,
        order: Direction,
        onlyVisible: Boolean,
        onlyUsable: Boolean,
        status: ProfileStatusEnum?,
        searched: String?,
        startAccess: ZonedDateTime?,
        endAccess: ZonedDateTime?
    ): Flux<EventProfileModel> {
        return repository.findEventProfilesByUserId(userId, onlyVisible, onlyUsable, status, startAccess, endAccess)
            .searchAndSort(order, searched, compareBy { it.event?.name })
    }

    override fun findAllUserEventProfiles(userId: UUID, onlyUsable: Boolean, status: ProfileStatusEnum?): Flux<EventProfileModel> {
        return repository.findAllEventProfilesByUserId(userId, onlyUsable, status)
    }

    override fun findUsableProfileByEventAndUserId(userId: UUID, eventId: UUID): Mono<EventProfileModel> {
        return repository.findUsableProfileByEventAndUserId(userId, eventId)
    }

    override fun findEventByBlockingLevel0RoleAndUserId(userId: UUID, onlyVisible: Boolean): Flux<EventProfileRoleCountModel> {
        return repository.findLevel0EventProfileRoleByUserId(userId, onlyVisible)
            .filter { Objects.isNull(it.level0) || it.level0 !! <= 1 }
    }

    override fun findUserEventProfileById(currentUser: UserModel, id: UUID, onlyVisible: Boolean): Mono<EventProfileModel> {
        return repository.findEventProfilesByIdAndUserId(currentUser.id !!, id, onlyVisible)
            .notFoundIfEmpty(id)
    }

    override fun <T: GenericModel> validateNotLastEventRoleLevel0(userId: UUID, eventId: UUID?, result: T, error: String): Mono<T> {
        return findEventByBlockingLevel0RoleAndUserId(userId, onlyVisible = true)
            .filter { Objects.isNull(eventId) || it.event?.id != eventId }
            .collectList()
            .handle { it, handle ->
                if (it.isNotEmpty()) {
                    log.warn("The user {} is the last administrator of {} event(s)", userId, it.size)
                    handle.error(RegistryException(FORBIDDEN, error))
                } else handle.next(result)
            }
    }

    override fun createUserEventProfileFromEvent(currentUser: UserModel, event: EventModel): Mono<EventProfileModel> {
        val profile = EventProfileModel().apply {
            this.event = event
            this.user = currentUser
            this.role = roleService.getLevel0RoleFromEventRoles()
            this.status = ACCEPTED
        }
        profile.create(currentUser)

        return repository.create(profile)
    }

    override fun updateUserEventProfileStatusById(
        currentUser: UserModel,
        id: UUID,
        status: ProfileStatusEnum
    ): Mono<EventProfileModel> {
        return repository.findEventProfilesByIdAndUserId(currentUser.id !!, id, onlyVisible = true)
            .filter { it.status == INVITED }
            .notFoundIfEmpty(id)
            .flatMap { profile ->
                profile.status = status
                profile.update(currentUser)
                repository.update(profile)
            }
    }

    override fun deleteUserEventProfileById(currentUser: UserModel, id: UUID): Mono<Void> {
        return repository.findEventProfilesByIdAndUserId(currentUser.id !!, id, onlyVisible = false)
            .validateNotLastEventRoleLevel0(EVENT_PROFILE_DELETE_LAST_EVENT_ADMINISTRATOR)
            .flatMap { repository.deleteById(id) }
    }

    private fun Mono<EventProfileModel>.validateNotLastEventRoleLevel0(error: String) = flatMap {
        validateNotLastEventRoleLevel0(it.user !!.id !!, it.event !!.id !!, it, error)
    }
}
