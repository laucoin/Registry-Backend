package fr.laucoin.registry.backend.domain.repository

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IParticipantModelRepository: IGenericReadEventModelRepository<ParticipantModel>,
                                       IGenericWriteModelRepository<ParticipantModel> {
    fun findPage(
        eventId: UUID,
        pageable: PageableModel,
        searchParams: ParticipantSearchParamModel,
    ): Mono<PageModel<ParticipantModel>>

    fun findPageByGroupId(
        eventId: UUID,
        groupId: UUID,
        pageable: PageableModel,
        searchParams: ParticipantSearchParamModel,
    ): Mono<PageModel<ParticipantModel>>

    fun findAllByIds(eventId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<ParticipantModel>

    fun findByUserId(eventId: UUID, userId: UUID): Flux<ParticipantModel>

    fun findWithLimit(limit: Int, eventId: UUID, searchParams: ParticipantSearchParamModel): Flux<ParticipantModel>

    fun updateAllEndAvailability(ids: List<UUID>, endAvailability: CustomDateTimeModel): Flux<ParticipantModel>

    fun saveAllGuest(guests: List<ParticipantModel>): Flux<ParticipantModel>

    fun deleteAll(ids: List<UUID>): Mono<Void>
}
