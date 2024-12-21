package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.UserModel
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IEventProfileService {
    fun findEventProfilesByEventId(
        eventId: UUID,
        order: Direction,
        onlyVisible: Boolean,
        status: ProfileStatusEnum?,
        searched: String?,
        startAccess: ZonedDateTime?,
        endAccess: ZonedDateTime?,
    ): Flux<EventProfileModel>

    fun findEventProfileByEventIdAndId(eventId: UUID, id: UUID, onlyVisible: Boolean): Mono<EventProfileModel>
    fun searchUsers(searched: String?): Flux<UserModel>
    fun getAssignableEventRoles(currentUser: UserModel, eventId: UUID): Mono<List<String>>
    fun createSupportEventProfile(currentUser: UserModel, eventId: UUID): Mono<EventProfileModel>
    fun createEventProfiles(
        currentUser: UserModel,
        eventId: UUID,
        userIds: List<UUID>,
        profiles: List<EventProfileModel>
    ): Flux<EventProfileModel>

    fun updateEventProfileById(currentUser: UserModel, eventId: UUID, id: UUID, profile: EventProfileModel): Mono<EventProfileModel>
    fun blockEventProfileById(currentUser: UserModel, eventId: UUID, id: UUID): Mono<EventProfileModel>
    fun unblockEventProfileById(currentUser: UserModel, eventId: UUID, id: UUID): Mono<EventProfileModel>
    fun deleteEventProfileById(currentUser: UserModel, eventId: UUID, id: UUID): Mono<Void>
}
