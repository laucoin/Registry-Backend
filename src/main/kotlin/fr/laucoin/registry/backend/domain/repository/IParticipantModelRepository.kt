package fr.laucoin.registry.backend.domain.repository

import fr.laucoin.registry.backend.domain.model.ParticipantModel
import java.time.ZonedDateTime
import java.util.UUID
import reactor.core.publisher.Flux

interface IParticipantModelRepository: IGenericReadEventModelRepository<ParticipantModel>,
                                       IGenericWriteModelRepository<ParticipantModel> {
    fun findAll(
        eventId: UUID,
        onlyVisible: Boolean,
        onlyPresent: Boolean,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?,
    ): Flux<ParticipantModel>

    fun findAllByIds(eventId: UUID, ids: List<UUID>, onlyVisible: Boolean): Flux<ParticipantModel>
}
