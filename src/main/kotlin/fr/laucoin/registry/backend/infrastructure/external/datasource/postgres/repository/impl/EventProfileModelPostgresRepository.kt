package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.EventProfileRoleCountModel
import fr.laucoin.registry.backend.domain.repository.IEventProfileModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.EventProfileEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.EventProfileRoleCountEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IEventProfileEntityRepository
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class EventProfileModelPostgresRepository(
    private val repository: IEventProfileEntityRepository,
    private val mapper: EventProfileEntityMapper,
    private val roleCountMapper: EventProfileRoleCountEntityMapper,
): IEventProfileModelRepository {
    override fun findEventProfilesByEventId(
        eventId: UUID,
        onlyVisible: Boolean,
        onlyUsable: Boolean,
        status: ProfileStatusEnum?,
        startAccess: ZonedDateTime?,
        endAccess: ZonedDateTime?
    ): Flux<EventProfileModel> {
        return repository.findByEventId(eventId, onlyVisible, onlyUsable, status, startAccess, endAccess)
            .map(mapper::toModel)
    }

    override fun findEventProfilesByUserId(
        userId: UUID,
        onlyVisible: Boolean,
        onlyUsable: Boolean,
        status: ProfileStatusEnum?,
        startAccess: ZonedDateTime?,
        endAccess: ZonedDateTime?
    ): Flux<EventProfileModel> {
        return repository.findByUserId(userId, onlyVisible, onlyUsable, status, startAccess, endAccess)
            .map(mapper::toModel)
    }

    override fun findAllEventProfilesByUserId(userId: UUID, onlyUsable: Boolean, status: ProfileStatusEnum?): Flux<EventProfileModel> {
        return repository.findAllByUserId(userId, onlyUsable, status)
            .map(mapper::toModel)
    }

    override fun findUsableProfileByEventAndUserId(userId: UUID, eventId: UUID): Mono<EventProfileModel> {
        return repository.findUsableProfileByEventAndUserId(userId, eventId)
            .map(mapper::toModel)
    }

    override fun findById(eventId: UUID, id: UUID, onlyVisible: Boolean): Mono<EventProfileModel> {
        return repository.findByIdAndEventId(eventId, id, onlyVisible).map(mapper::toModel)
    }

    override fun findEventProfilesByIdAndUserId(userId: UUID, id: UUID, onlyVisible: Boolean): Mono<EventProfileModel> {
        return repository.findByIdAndUserId(userId, id, onlyVisible).map(mapper::toModel)
    }

    override fun findEventProfileByEventAndUserId(
        eventId: UUID,
        userId: UUID,
        onlyVisible: Boolean,
        onlyUsable: Boolean,
        status: ProfileStatusEnum?,
    ): Mono<EventProfileModel> {
        return repository.findEventProfileByEventAndUserId(eventId, userId, onlyVisible, onlyUsable, status).map(mapper::toModel)
    }

    override fun findLevel0EventProfileRoleByUserId(userId: UUID, onlyVisible: Boolean): Flux<EventProfileRoleCountModel> {
        return repository.findLevel0EventProfileRoleByUserId(userId, onlyVisible).map(roleCountMapper::toModel)
    }

    override fun findLevel0EventProfileRoleByEventId(eventId: UUID, onlyVisible: Boolean): Flux<EventProfileModel> {
        return repository.findLevel0EventProfileRoleByEventId(eventId, onlyVisible).map(mapper::toModel)
    }

    override fun save(element: EventProfileModel): Mono<EventProfileModel> {
        return repository.save(mapper.toEntity(element)).map(mapper::toModel)
    }

    override fun saveAll(profiles: List<EventProfileModel>): Flux<EventProfileModel> {
        return repository.saveAll(profiles.map(mapper::toEntity)).map(mapper::toModel)
    }

    override fun deleteById(id: UUID): Mono<Void> {
        return repository.deleteById(id)
    }
}
