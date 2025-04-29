package fr.laucoin.registry.backend.domain.repository

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IParticipantModelRepository: IGenericReadProjectModelRepository<ParticipantModel>,
                                       IGenericWriteModelRepository<ParticipantModel> {
    fun findPage(
        projectId: UUID,
        pageable: PageableModel,
        searchParams: ParticipantSearchParamModel,
    ): Mono<PageModel<ParticipantModel>>

    fun findPageByGroupId(
        projectId: UUID,
        groupId: UUID,
        pageable: PageableModel,
        searchParams: ParticipantSearchParamModel,
    ): Mono<PageModel<ParticipantModel>>

    fun findAllByIds(projectId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<ParticipantModel>

    fun findByUserId(projectId: UUID, userId: UUID): Flux<ParticipantModel>

    fun findWithLimit(limit: Int, projectId: UUID, searchParams: ParticipantSearchParamModel): Flux<ParticipantModel>

    fun updateAllEndAvailability(ids: List<UUID>, endAvailability: CustomDateTimeModel): Flux<ParticipantModel>

    fun saveAllGuest(guests: List<ParticipantModel>): Flux<ParticipantModel>

    fun deleteAll(ids: List<UUID>): Mono<Void>
}
