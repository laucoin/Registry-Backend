package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import java.time.LocalDate
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IVehicleService {
	fun findVehiclesPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: VehicleSearchParamModel,
	): Mono<PageModel<VehicleModel>>

	fun findVehicleById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<VehicleModel>

	fun findVehicleMovementsPage(
		projectId: UUID,
		id: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
	): Mono<PageModel<MovementModel>>

	fun createVehicle(currentUser: CurrentUserModel, vehicle: VehicleModel): Mono<VehicleModel>
	fun updateVehicleById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		vehicle: VehicleModel
	): Mono<VehicleModel>

	fun disableVehicleById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<VehicleModel>
	fun enableVehicleById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<VehicleModel>
	fun deleteVehicleById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Unit>
	fun purgeVehiclesIfNecessary(dateThreshold: LocalDate, dryRun: Boolean): Flux<UUID>
}
