package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_DELETE_HAS_MOVEMENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.repository.IMovementModelRepository
import fr.laucoin.registry.backend.domain.repository.IVehicleModelRepository
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IEventService
import fr.laucoin.registry.backend.domain.service.IVehicleService
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class VehicleService(
    private val repository: IVehicleModelRepository,
    private val eventService: IEventService,
    private val movementRepository: IMovementModelRepository,
): IVehicleService, GenericService() {
    override fun findVehiclesByEventId(
        eventId: UUID,
        order: Direction,
        onlyVisible: Boolean,
        onlyPresent: Boolean,
        searched: String?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ): Flux<VehicleModel> {
        return repository.findAll(eventId, onlyVisible, onlyPresent, startDateTime, endDateTime)
            .searchAndSort(order, searched, compareBy { it.registration })
    }

    override fun findVehicleById(eventId: UUID, id: UUID, onlyVisible: Boolean): Mono<VehicleModel> {
        return repository.findById(eventId, id, onlyVisible)
            .notFoundIfEmpty(id)
    }

    override fun findVehicleMovements(
        eventId: UUID,
        id: UUID,
        order: Direction,
        onlyVisible: Boolean,
        searched: String?,
        type: MovementTypeEnum?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ): Flux<MovementModel> {
        return movementRepository.findAll(
            eventId,
            onlyVisible,
            type,
            startDateTime,
            endDateTime
        )
            .filter { it.content.any { c -> c.vehicle?.id == id } }
            .searchAndSort(order, searched, compareBy { it.dateTime })
    }

    override fun createVehicle(currentUser: UserModel, vehicle: VehicleModel): Mono<VehicleModel> {
        return eventService.validateDateTimes(
            vehicle.event !!.id !!,
            vehicle.begin,
            vehicle.end,
            VEHICLE_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE,
        )
            .flatMap { repository.create(vehicle.apply { create(currentUser) }) }
    }

    override fun updateVehicleById(
        currentUser: UserModel,
        eventId: UUID,
        id: UUID,
        vehicle: VehicleModel
    ): Mono<VehicleModel> {
        return eventService.validateDateTimes(
            vehicle.event !!.id !!,
            vehicle.begin,
            vehicle.end,
            VEHICLE_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE,
        )
            .flatMap { findVehicleById(eventId, id, onlyVisible = false) }
            .map {
                it.apply {
                    registration = vehicle.registration
                    model = vehicle.model
                    brand = vehicle.brand
                    begin = vehicle.begin
                    end = vehicle.end
                }
            }
            .updateVehicle(currentUser)
    }

    override fun disableVehicleById(currentUser: UserModel, eventId: UUID, id: UUID): Mono<VehicleModel> {
        return findVehicleById(eventId, id, onlyVisible = true)
            .updateVisibility(visibility = false)
            .updateVehicle(currentUser)
    }

    override fun enableVehicleById(currentUser: UserModel, eventId: UUID, id: UUID): Mono<VehicleModel> {
        return findVehicleById(eventId, id, onlyVisible = false)
            .updateVisibility(visibility = true)
            .updateVehicle(currentUser)
    }

    override fun deleteVehicleById(currentUser: UserModel, eventId: UUID, id: UUID): Mono<Void> {
        return findVehicleById(eventId, id, onlyVisible = false)
            .validateHasNoMovementLinked(VEHICLE_DELETE_HAS_MOVEMENT)
            .flatMap { repository.deleteById(it.id !!) }
    }

    private fun Mono<VehicleModel>.updateVehicle(currentUser: UserModel) = flatMap {
        repository.update(it.apply { update(currentUser) })
    }

    private fun Mono<VehicleModel>.validateHasNoMovementLinked(error: String) = flatMap { vehicleToUpdate ->
        findVehicleMovements(
            vehicleToUpdate.event !!.id !!,
            vehicleToUpdate.id !!,
            order = ASC,
            onlyVisible = false,
            searched = null,
            type = null,
            startDateTime = null,
            endDateTime = null
        )
            .collectList()
            .handle { it, handle ->
                if (it.isNotEmpty()) {
                    log.warn("The vehicle {} already linked to movement(s)", vehicleToUpdate.id)
                    handle.error(RegistryException(FORBIDDEN, error))
                } else handle.next(vehicleToUpdate)
            }
    }
}
