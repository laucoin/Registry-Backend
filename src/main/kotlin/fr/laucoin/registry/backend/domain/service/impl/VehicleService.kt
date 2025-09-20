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
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.port.IVehiclePort
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.domain.service.IVehicleService
import java.time.LocalDate
import java.util.UUID
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class VehicleService(
	private val port: IVehiclePort,
	private val projectService: IProjectService,
	private val movementPort: IMovementPort,
): IVehicleService, GenericService() {
	override fun findVehiclesPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: VehicleSearchParamModel,
	): Mono<PageModel<VehicleModel>> {
		return port.findPage(projectId, pageable, searchParams)
	}

	override fun findVehicleById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<VehicleModel> {
		return port.findById(projectId, id, visibilitySearched)
			.notFoundIfEmpty(id)
	}

	override fun findVehicleMovementsPage(
		projectId: UUID,
		id: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
	): Mono<PageModel<MovementModel>> {
		return movementPort.findPageByVehicleId(projectId, id, pageable, searchParams)
	}

	override fun createVehicle(currentUser: CurrentUserModel, vehicle: VehicleModel): Mono<VehicleModel> {
		return projectService.validateDateTimes(
			vehicle.project!!.id!!,
			vehicle.startAvailability,
			vehicle.endAvailability,
			VEHICLE_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE,
		)
			.flatMap { port.create(vehicle.apply { create(currentUser) }) }
	}

	override fun updateVehicleById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		vehicle: VehicleModel
	): Mono<VehicleModel> {
		return projectService.validateDateTimes(
			vehicle.project!!.id!!,
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
			.flatMap { port.deleteById(it.id!!) }
	}

	override fun purgeVehiclesIfNecessary(dateThreshold: LocalDate, dryRun: Boolean): Flux<UUID> {
		log.info("Purging vehicles unused since {}", dateThreshold)
		return port.findUnusedSince(dateThreshold)
			.flatMap {
				if (dryRun) {
					log.info("[Dry run] vehicle {} would be deleted", it)
					Mono.just(it)
				} else {
					log.info("Purging vehicle {}", it)
					port.deleteById(it).thenReturn(it)
						.doOnNext { e -> log.info("{} vehicle was deleted", e) }
						.doOnError { err -> log.error("Failed to purge vehicle", err) }
				}
			}
	}

	private fun Mono<VehicleModel>.updateVehicle(currentUser: CurrentUserModel) = flatMap {
		port.update(it.apply { update(currentUser) })
	}

	private fun Mono<VehicleModel>.validateHasNoMovementLinked(error: String) = flatMap { vehicleToUpdate ->
		movementPort.countAllByVehicleId(
			vehicleToUpdate.project!!.id!!,
			vehicleToUpdate.id!!,
			MovementSearchParamModel(),
		).handle { it, handle ->
			if (it > 0) {
				log.warn("The vehicle {} already linked to movement(s)", vehicleToUpdate.id)
				handle.error(RegistryException(CONFLICT, error))
			} else handle.next(vehicleToUpdate)
		}
	}
}
