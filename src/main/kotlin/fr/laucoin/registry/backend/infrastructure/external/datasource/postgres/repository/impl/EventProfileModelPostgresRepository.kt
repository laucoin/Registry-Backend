package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.EventProfileRoleCountModel
import fr.laucoin.registry.backend.domain.model.EventProfileRoleModel
import fr.laucoin.registry.backend.domain.model.EventProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.repository.IEventProfileModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.EventProfileEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.EventProfileRoleCountEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.EventProfileRoleEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IEventProfileEntityRepository
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class EventProfileModelPostgresRepository(
    private val repository: IEventProfileEntityRepository,
    private val mapper: EventProfileEntityMapper,
    private val roleMapper: EventProfileRoleEntityMapper,
    private val roleCountMapper: EventProfileRoleCountEntityMapper,
): IEventProfileModelRepository {
    override fun findEventProfilesPageByUserId(
        userId: UUID,
        pageable: PageableModel,
        searchParams: EventProfileSearchParamModel
    ): Mono<PageModel<EventProfileModel>> {
        return Mono.zip(
            repository.countByUserId(
                userId,
                searchParams.textSearched,
                searchParams.visibilitySearched,
                searchParams.availabilitySearched,
                searchParams.statusSearched,
                searchParams.dateTimeSearched,
            ),
            repository.findByUserId(
                userId,
                searchParams.textSearched,
                searchParams.visibilitySearched,
                searchParams.availabilitySearched,
                searchParams.statusSearched,
                searchParams.dateTimeSearched,
                pageable.limit,
                pageable.offset,
            ).map(mapper::toModel).collectList(),
        ).map {
            PageModel(pageable, it.t1, it.t2)
        }
    }

    override fun findEventProfilesPageByEventId(
        eventId: UUID,
        pageable: PageableModel,
        searchParams: EventProfileSearchParamModel,
    ): Mono<PageModel<EventProfileModel>> {
        return Mono.zip(
            repository.countByEventId(
                eventId,
                searchParams.textSearched,
                searchParams.visibilitySearched,
                searchParams.availabilitySearched,
                searchParams.statusSearched,
                searchParams.dateTimeSearched,
            ),
            repository.findByEventId(
                eventId,
                searchParams.textSearched,
                searchParams.visibilitySearched,
                searchParams.availabilitySearched,
                searchParams.statusSearched,
                searchParams.dateTimeSearched,
                pageable.limit,
                pageable.offset,
            ).map(mapper::toModel).collectList(),
        ).map {
            PageModel(pageable, it.t1, it.t2)
        }
    }

    override fun findUserIdsWithEventProfileForEventWithProfileExclusion(
        eventId: UUID,
        userIds: List<UUID>,
        profileIdToExclude: UUID?,
        statusSearched: List<ProfileStatusEnum>,
        startDateTimeSearched: LocalDateTime?,
        endDateTimeSearched: LocalDateTime?,
    ): Flux<UUID> {
        if (userIds.isEmpty()) return Flux.empty()
        return repository.findUserIdsWithEventProfileForEventWithProfileExclusion(
            eventId,
            userIds,
            profileIdToExclude,
            statusSearched,
            startDateTimeSearched,
            endDateTimeSearched
        )
    }

    override fun findEventProfilesRolesByUserId(userId: UUID): Flux<EventProfileRoleModel> {
        return repository.findAllRolesByUserId(
            userId,
            visibilitySearched = null,
            availabilitySearched = true,
            statusSearched = listOf(ACCEPTED),
        ).map(roleMapper::toModel)
    }

    override fun findEventProfileByUserIdAndId(userId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<EventProfileModel> {
        return repository.findByUserIdAndId(userId, id, visibilitySearched)
            .map(mapper::toModel)
            .switchIfEmpty(Mono.empty())
    }

    override fun findById(eventId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<EventProfileModel> {
        return repository.findByEventIdAndId(eventId, id, visibilitySearched)
            .map(mapper::toModel)
            .switchIfEmpty(Mono.empty())
    }

    override fun findEventProfileByEventAndUserId(
        eventId: UUID,
        userId: UUID,
        searchParams: EventProfileSearchParamModel,
    ): Mono<EventProfileModel> {
        return repository.findEventProfileByEventAndUserId(
            eventId,
            userId,
            searchParams.visibilitySearched,
            searchParams.availabilitySearched,
            searchParams.statusSearched,
        )
            .map(mapper::toModel)
            .switchIfEmpty(Mono.empty())
    }

    override fun findLevel0EventProfileRoleByUserId(userId: UUID, visibilitySearched: Boolean?): Flux<EventProfileRoleCountModel> {
        return repository.findLevel0EventProfileRoleByUserId(userId, visibilitySearched).map(roleCountMapper::toModel)
    }

    override fun findLevel0EventProfileRoleByEventId(eventId: UUID, visibilitySearched: Boolean?): Flux<EventProfileModel> {
        return repository.findLevel0EventProfileRoleByEventId(eventId, visibilitySearched).map(mapper::toModel)
    }

    override fun create(element: EventProfileModel): Mono<EventProfileModel> {
        return save(element)
    }

    override fun update(element: EventProfileModel): Mono<EventProfileModel> {
        return save(element)
    }

    private fun save(element: EventProfileModel): Mono<EventProfileModel> {
        return repository.save(mapper.toEntity(element)).map(mapper::toModel)
    }

    override fun saveAll(profiles: List<EventProfileModel>): Flux<EventProfileModel> {
        return repository.saveAll(profiles.map(mapper::toEntity)).map(mapper::toModel)
    }

    override fun deleteById(id: UUID): Mono<Void> {
        return repository.deleteById(id)
    }
}
