package fr.laucoin.registry.backend.domain.repository

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.EventProfileRoleCountModel
import fr.laucoin.registry.backend.domain.model.EventProfileRoleModel
import fr.laucoin.registry.backend.domain.model.EventProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import java.time.LocalDateTime
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IEventProfileModelRepository: IGenericReadEventModelRepository<EventProfileModel>,
                                        IGenericWriteModelRepository<EventProfileModel> {
    fun findEventProfilesPageByUserId(
        userId: UUID,
        pageable: PageableModel,
        searchParams: EventProfileSearchParamModel,
    ): Mono<PageModel<EventProfileModel>>

    fun findEventProfilesPageByEventId(
        eventId: UUID,
        pageable: PageableModel,
        searchParams: EventProfileSearchParamModel,
    ): Mono<PageModel<EventProfileModel>>

    fun findUserIdsWithEventProfileForEventWithProfileExclusion(
        eventId: UUID,
        userIds: List<UUID>,
        profileIdToExclude: UUID?,
        statusSearched: List<ProfileStatusEnum> = ProfileStatusEnum.entries.toList(),
        startDateTimeSearched: LocalDateTime? = null,
        endDateTimeSearched: LocalDateTime? = null,
    ): Flux<UUID>

    fun findEventProfilesRolesByUserId(userId: UUID): Flux<EventProfileRoleModel>

    fun findEventProfileByUserIdAndId(userId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<EventProfileModel>

    fun findEventProfileByEventAndUserId(
        eventId: UUID,
        userId: UUID,
        searchParams: EventProfileSearchParamModel,
    ): Mono<EventProfileModel>

    fun findLevel0EventProfileRoleByUserId(userId: UUID, visibilitySearched: Boolean?): Flux<EventProfileRoleCountModel>

    fun findLevel0EventProfileRoleByEventId(eventId: UUID, visibilitySearched: Boolean?): Flux<EventProfileModel>

    fun saveAll(profiles: List<EventProfileModel>): Flux<EventProfileModel>
}
