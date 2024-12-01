package fr.laucoin.registry.backend.domain.repository

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.model.MovementModel
import java.time.ZonedDateTime
import java.util.UUID
import reactor.core.publisher.Flux

interface IMovementModelRepository: IGenericReadEventModelRepository<MovementModel>, IGenericWriteModelRepository<MovementModel> {
    fun findAll(
        eventId: UUID,
        onlyVisible: Boolean,
        type: MovementTypeEnum?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ): Flux<MovementModel>
}
