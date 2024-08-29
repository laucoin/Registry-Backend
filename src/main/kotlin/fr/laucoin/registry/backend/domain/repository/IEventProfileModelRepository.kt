package fr.laucoin.registry.backend.domain.repository

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.EventProfileRoleCountModel
import java.time.ZonedDateTime
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IEventProfileModelRepository: IGenericReadEventModelRepository<EventProfileModel>,
                                        IGenericWriteModelRepository<EventProfileModel> {
    fun findEventProfilesByEventId(
        eventId: UUID,
        onlyVisible: Boolean,
        onlyUsable: Boolean,
        status: ProfileStatusEnum?,
        startAccess: ZonedDateTime?,
        endAccess: ZonedDateTime?,
    ): Flux<EventProfileModel>

    fun findEventProfilesByUserId(
        userId: UUID,
        onlyVisible: Boolean,
        onlyUsable: Boolean,
        status: ProfileStatusEnum?,
        startAccess: ZonedDateTime?,
        endAccess: ZonedDateTime?,
    ): Flux<EventProfileModel>

    fun findAllEventProfilesByUserId(
        userId: UUID,
        onlyUsable: Boolean,
        status: ProfileStatusEnum?,
    ): Flux<EventProfileModel>

    fun findUsableProfileByEventAndUserId(
        userId: UUID,
        eventId: UUID,
    ): Mono<EventProfileModel>

    fun findEventProfilesByIdAndUserId(userId: UUID, id: UUID, onlyVisible: Boolean): Mono<EventProfileModel>

    fun findEventProfileByEventAndUserId(
        eventId: UUID,
        userId: UUID,
        onlyVisible: Boolean,
        onlyUsable: Boolean,
        status: ProfileStatusEnum?,
    ): Mono<EventProfileModel>

    fun findLevel0EventProfileRoleByUserId(userId: UUID, onlyVisible: Boolean): Flux<EventProfileRoleCountModel>

    fun findLevel0EventProfileRoleByEventId(eventId: UUID, onlyVisible: Boolean): Flux<EventProfileModel>

    fun saveAll(profiles: List<EventProfileModel>): Flux<EventProfileModel>
}
