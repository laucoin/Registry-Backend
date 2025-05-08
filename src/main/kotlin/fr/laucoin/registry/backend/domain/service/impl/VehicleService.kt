package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_DELETE_HAS_MOVEMENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.repository.IMovementModelRepository
import fr.laucoin.registry.backend.domain.repository.IVehicleModelRepository
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.domain.service.IVehicleService
import java.util.UUID
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class VehicleService(
    private val repository: IVehicleModelRepository,
    private val projectService: IProjectService,
    private val movementRepository: IMovementModelRepository,
): IVehicleService, GenericService() {
    override fun findVehiclesPage(
        projectId: UUID,
        pageable: PageableModel,
        searchParams: VehicleSearchParamModel,
    ): Mono<PageModel<VehicleModel>> {
        return repository.findPage(projectId, pageable, searchParams)
    }

    override fun findVehicleById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<VehicleModel> {
        return repository.findById(projectId, id, visibilitySearched)
            .notFoundIfEmpty(id)
    }

    override fun findVehicleMovementsPage(
        projectId: UUID,
        id: UUID,
        pageable: PageableModel,
        searchParams: MovementSearchParamModel,
    ): Mono<PageModel<MovementModel>> {
        return movementRepository.findPageByVehicleId(projectId, id, pageable, searchParams)
    }

    override fun createVehicle(currentUser: CurrentUserModel, vehicle: VehicleModel): Mono<VehicleModel> {
        return projectService.validateDateTimes(
            vehicle.project !!.id !!,
            vehicle.startAvailability,
            vehicle.endAvailability,
            VEHICLE_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE,
        )
            .flatMap { repository.create(vehicle.apply { create(currentUser) }) }
    }

    override fun updateVehicleById(
        currentUser: CurrentUserModel,
        projectId: UUID,
        id: UUID,
        vehicle: VehicleModel
    ): Mono<VehicleModel> {
        return projectService.validateDateTimes(
            vehicle.project !!.id !!,
            vehicle.startAvailability,
            vehicle.endAvailability,
            VEHICLE_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE,
        )
            .flatMap { findVehicleById(projectId, id, visibilitySearched = null) }
            .map {
                it.apply {
                    licensePlate = vehicle.licensePlate
                    model = vehicle.model
                    brand = vehicle.brand
                    startAvailability = vehicle.startAvailability
                    endAvailability = vehicle.endAvailability
                }
            }
            .updateVehicle(currentUser)
    }

    override fun disableVehicleById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<VehicleModel> {
        return findVehicleById(projectId, id, visibilitySearched = true)
            .updateVisibility(visibility = false)
            .updateVehicle(currentUser)
    }

    override fun enableVehicleById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<VehicleModel> {
        return findVehicleById(projectId, id, visibilitySearched = false)
            .updateVisibility(visibility = true)
            .updateVehicle(currentUser)
    }

    override fun deleteVehicleById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Void> {
        return findVehicleById(projectId, id, visibilitySearched = null)
            .validateHasNoMovementLinked(VEHICLE_DELETE_HAS_MOVEMENT)
            .flatMap { repository.deleteById(it.id !!) }
    }

    private fun Mono<VehicleModel>.updateVehicle(currentUser: CurrentUserModel) = flatMap {
        repository.update(it.apply { update(currentUser) })
    }

    private fun Mono<VehicleModel>.validateHasNoMovementLinked(error: String) = flatMap { vehicleToUpdate ->
        movementRepository.countAllByVehicleId(
            vehicleToUpdate.project !!.id !!,
            vehicleToUpdate.id !!,
            MovementSearchParamModel(),
        ).handle { it, handle ->
            if (it > 0) {
                log.warn("The vehicle {} already linked to movement(s)", vehicleToUpdate.id)
                handle.error(RegistryException(FORBIDDEN, error))
            } else handle.next(vehicleToUpdate)
        }
    }
}
