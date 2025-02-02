package fr.laucoin.registry.backend.domain.repository

import fr.laucoin.registry.backend.domain.model.VehicleModel
import java.time.ZonedDateTime
import java.util.UUID
import reactor.core.publisher.Flux

interface IVehicleModelRepository: IGenericReadEventModelRepository<VehicleModel>,
                                   IGenericWriteModelRepository<VehicleModel> {
    fun findAll(
        eventId: UUID,
        onlyVisible: Boolean,
        onlyPresent: Boolean,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?,
    ): Flux<VehicleModel>

    fun findAllByIds(eventId: UUID, ids: List<UUID>, onlyVisible: Boolean): Flux<VehicleModel>
}
