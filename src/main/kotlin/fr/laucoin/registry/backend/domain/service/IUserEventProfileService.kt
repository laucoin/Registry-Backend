package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.EventProfileRoleCountModel
import fr.laucoin.registry.backend.domain.model.GenericModel
import fr.laucoin.registry.backend.domain.model.UserModel
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IUserEventProfileService {
    fun findUserEventProfiles(
        userId: UUID,
        order: Direction,
        onlyVisible: Boolean,
        onlyUsable: Boolean,
        status: ProfileStatusEnum?,
        searched: String?,
        startAccess: ZonedDateTime?,
        endAccess: ZonedDateTime?,
    ): Flux<EventProfileModel>

    fun findAllUserEventProfiles(
        userId: UUID,
        onlyUsable: Boolean,
        status: ProfileStatusEnum?,
    ): Flux<EventProfileModel>

    fun findUsableProfileByEventAndUserId(
        userId: UUID,
        eventId: UUID
    ): Mono<EventProfileModel>

    fun findEventByBlockingLevel0RoleAndUserId(userId: UUID, onlyVisible: Boolean): Flux<EventProfileRoleCountModel>
    fun findUserEventProfileById(currentUser: UserModel, id: UUID, onlyVisible: Boolean): Mono<EventProfileModel>
    fun <T: GenericModel> validateNotLastEventRoleLevel0(userId: UUID, eventId: UUID?, result: T, error: String): Mono<T>
    fun createUserEventProfileFromEvent(currentUser: UserModel, event: EventModel): Mono<EventProfileModel>
    fun updateUserEventProfileStatusById(currentUser: UserModel, id: UUID, status: ProfileStatusEnum): Mono<EventProfileModel>
    fun deleteUserEventProfileById(currentUser: UserModel, id: UUID): Mono<Void>
}
