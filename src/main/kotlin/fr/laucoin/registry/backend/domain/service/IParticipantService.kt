package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.UserModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IParticipantService {
    fun findParticipantsPage(
        eventId: UUID,
        pageable: PageableModel,
        searchParams: ParticipantSearchParamModel,
    ): Mono<PageModel<ParticipantModel>>

    fun findParticipantsByIds(eventId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<ParticipantModel>
    fun findParticipantById(eventId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<ParticipantModel>
    fun searchUsers(eventId: UUID, textSearched: String?): Flux<UserModel>
    fun searchGroups(eventId: UUID, textSearched: String?): Flux<GroupModel>

    fun findParticipantMovementsPage(
        eventId: UUID,
        id: UUID,
        pageable: PageableModel,
        searchParams: MovementSearchParamModel,
    ): Mono<PageModel<MovementModel>>

    fun createParticipant(currentUser: CurrentUserModel, participant: ParticipantModel): Mono<ParticipantModel>
    fun updateParticipantById(
        currentUser: CurrentUserModel,
        eventId: UUID,
        id: UUID,
        participant: ParticipantModel
    ): Mono<ParticipantModel>

    fun disableParticipantById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<ParticipantModel>
    fun enableParticipantById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<ParticipantModel>
    fun deleteParticipantById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<Void>
}
