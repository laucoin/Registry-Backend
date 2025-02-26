package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IVehicleService {
    fun findVehiclesByEventId(
        eventId: UUID,
        order: Direction,
        onlyVisible: Boolean,
        onlyPresent: Boolean,
        searched: String?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?,
    ): Flux<VehicleModel>

    fun findVehicleById(eventId: UUID, id: UUID, onlyVisible: Boolean): Mono<VehicleModel>
    fun findVehicleMovements(
        eventId: UUID,
        id: UUID,
        order: Direction,
        onlyVisible: Boolean,
        searched: String?,
        type: MovementTypeEnum?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ): Flux<MovementModel>

    fun createVehicle(currentUser: UserModel, vehicle: VehicleModel): Mono<VehicleModel>
    fun updateVehicleById(currentUser: UserModel, eventId: UUID, id: UUID, vehicle: VehicleModel): Mono<VehicleModel>
    fun disableVehicleById(currentUser: UserModel, eventId: UUID, id: UUID): Mono<VehicleModel>
    fun enableVehicleById(currentUser: UserModel, eventId: UUID, id: UUID): Mono<VehicleModel>
    fun deleteVehicleById(currentUser: UserModel, eventId: UUID, id: UUID): Mono<Void>
}
