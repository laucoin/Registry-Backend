package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.UserModel
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IParticipantService {
    fun findParticipantsByEventId(
        eventId: UUID,
        order: Direction,
        onlyVisible: Boolean,
        onlyPresent: Boolean,
        searched: String?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?,
    ): Flux<ParticipantModel>

    fun findParticipantById(eventId: UUID, id: UUID, onlyVisible: Boolean): Mono<ParticipantModel>
    fun createParticipant(currentUser: UserModel, participant: ParticipantModel): Mono<ParticipantModel>
    fun updateParticipantById(currentUser: UserModel, eventId: UUID, id: UUID, participant: ParticipantModel): Mono<ParticipantModel>
    fun disableParticipantById(currentUser: UserModel, eventId: UUID, id: UUID): Mono<ParticipantModel>
    fun enableParticipantById(currentUser: UserModel, eventId: UUID, id: UUID): Mono<ParticipantModel>
    fun deleteParticipantById(currentUser: UserModel, eventId: UUID, id: UUID): Mono<Void>
}
