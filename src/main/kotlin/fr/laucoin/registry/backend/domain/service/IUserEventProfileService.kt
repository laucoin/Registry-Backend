package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.EventProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.GenericModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import java.util.UUID
import reactor.core.publisher.Mono

interface IUserEventProfileService {
    fun findEventProfilesPage(
        userId: UUID,
        pageable: PageableModel,
        searchParams: EventProfileSearchParamModel,
    ): Mono<PageModel<EventProfileModel>>

    fun findUserEventProfileById(currentUser: CurrentUserModel, id: UUID, visibilitySearched: Boolean?): Mono<EventProfileModel>
    fun <T: GenericModel> validateNotLastEventRoleLevel0(userId: UUID, eventId: UUID?, result: T, error: String): Mono<T>
    fun createUserEventProfileFromEvent(currentUser: CurrentUserModel, event: EventModel): Mono<EventProfileModel>
    fun updateUserEventProfileStatusById(currentUser: CurrentUserModel, id: UUID, status: ProfileStatusEnum): Mono<EventProfileModel>
    fun createSupportEventProfile(currentUser: CurrentUserModel, eventId: UUID): Mono<EventProfileModel>
    fun deleteUserEventProfileById(currentUser: CurrentUserModel, id: UUID): Mono<Void>
}
