package com.laucoin.registry.domain.profile.datasource.postgres.repository

import com.laucoin.registry.core.util.Logger
import com.laucoin.registry.core.util.notFoundIfEmpty
import com.laucoin.registry.domain.profile.datasource.postgres.model.ProfileDto
import com.laucoin.registry.domain.profile.datasource.postgres.repository.util.IProfileDtoRepository
import com.laucoin.registry.domain.profile.model.EnrichedProfileModel
import com.laucoin.registry.domain.profile.model.ProfileModel
import com.laucoin.registry.domain.profile.repository.IProfileModelRepository
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class ProfileModelRepository(
    private val repository: IProfileDtoRepository
): IProfileModelRepository, Logger() {
    override fun getAllByActiveAndUserId(
        userId: UUID,
        active: Boolean,
        accepted: Boolean,
        onlyVisible: Boolean
    ): Flux<EnrichedProfileModel> {
        log.debug(
            "Finding profiles by user ID \"{}\", active \"{}\", accepted \"{}\" and onlyVisible \"{}\"",
            userId,
            active,
            accepted,
            onlyVisible
        )
        return repository.getAllByActiveAndUserId(userId, active, accepted, onlyVisible)
            .map { it.toModel() }
    }

    override fun getAllByOutdatedAndUserId(
        userId: UUID,
        outdated: Boolean,
        accepted: Boolean,
        onlyVisible: Boolean
    ): Flux<EnrichedProfileModel> {
        log.debug(
            "Getting all profiles by user ID \"{}\", active \"{}\", accepted \"{}\" and onlyVisible \"{}\"",
            userId,
            outdated,
            accepted,
            onlyVisible
        )
        return repository.getAllByOutdatedAndUserId(userId, outdated, accepted, onlyVisible)
            .map { it.toModel() }
    }

    override fun getAllByActiveAndEventId(
        eventId: UUID,
        active: Boolean,
        accepted: Boolean,
        onlyVisible: Boolean
    ): Flux<EnrichedProfileModel> {
        log.debug(
            "Finding profiles by event ID \"{}\", active \"{}\", accepted \"{}\" and onlyVisible \"{}\"",
            eventId,
            active,
            accepted,
            onlyVisible
        )
        return repository.getAllByActiveAndEventId(eventId, active, accepted, onlyVisible)
            .map { it.toModel() }
    }

    override fun getAllByOutdatedAndEventId(
        eventId: UUID,
        outdated: Boolean,
        accepted: Boolean,
        onlyVisible: Boolean
    ): Flux<EnrichedProfileModel> {
        log.debug(
            "Getting all profiles by event ID \"{}\", active \"{}\", accepted \"{}\" and onlyVisible \"{}\"",
            eventId,
            outdated,
            accepted,
            onlyVisible
        )
        return repository.getAllByOutdatedAndEventId(eventId, outdated, accepted, onlyVisible)
            .map { it.toModel() }
    }

    override fun createAll(elements: Iterable<ProfileModel>): Flux<ProfileModel> {
        log.debug("Creating profiles \"{}\"", elements)
        return repository.saveAll(elements.map { ProfileDto(it) })
            .map { it.toModel() }
    }

    override fun getAll(onlyVisible: Boolean): Flux<EnrichedProfileModel> {
        log.debug("Getting all profiles onlyVisible \"{}\"", onlyVisible)
        return repository.getAll(onlyVisible)
            .map { it.toModel() }
    }

    override fun findById(id: UUID, onlyVisible: Boolean): Mono<EnrichedProfileModel> {
        log.debug("Finding onlyVisible \"{}\" profile by ID \"{}\"", onlyVisible, id)
        return repository.findById(id, onlyVisible)
            .notFoundIfEmpty(id)
            .map { it.toModel() }
    }

    override fun create(element: ProfileModel): Mono<ProfileModel> {
        log.debug("Creating profile \"{}\"", element)
        return repository.save(ProfileDto(element))
            .map { it.toModel() }
    }

    override fun updateById(id: UUID, element: ProfileModel): Mono<ProfileModel> {
        element.id = id
        log.debug("Updating profile by ID \"{}\" with values \"{}\"", id, element)
        return repository.save(ProfileDto(element))
            .map { it.toModel() }
    }

    override fun deleteById(id: UUID): Mono<Void> {
        log.debug("Deleting profile by ID \"{}\"", id)
        return repository.deleteById(id)
    }
}
