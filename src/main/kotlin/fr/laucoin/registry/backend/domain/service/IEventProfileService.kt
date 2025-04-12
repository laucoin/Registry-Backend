package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.EventProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.UserModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IEventProfileService {
    fun findEventProfilesPage(
        eventId: UUID,
        pageable: PageableModel,
        searchParams: EventProfileSearchParamModel,
    ): Mono<PageModel<EventProfileModel>>

    fun findEventProfileById(eventId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<EventProfileModel>
    fun searchUsers(textSearched: String?): Flux<UserModel>
    fun getAssignableEventRoles(currentUser: CurrentUserModel, eventId: UUID): Flux<String>
    fun createEventProfiles(
        currentUser: CurrentUserModel,
        eventId: UUID,
        userIds: List<UUID>,
        profiles: List<EventProfileModel>
    ): Mono<Pair<List<UUID>, List<UUID>>>

    fun updateEventProfileById(
        currentUser: CurrentUserModel,
        eventId: UUID,
        id: UUID,
        profile: EventProfileModel
    ): Mono<EventProfileModel>

    fun blockEventProfileById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<EventProfileModel>
    fun unblockEventProfileById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<EventProfileModel>
    fun deleteEventProfileById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<Void>
}
