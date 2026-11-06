package fr.laucoin.registry.backend.domain.service.usecase

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.model.VehicleTimelineModel
import java.util.UUID
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat de cas d'usage des véhicules exposé à la couche REST.
 * Périmètre : Reprend les opérations du service Vehicle appelées par les contrôleurs.
 * Limites : Contrat uniquement ; les règles métier vivent dans le service.
 */
interface IVehicleUseCase {
	fun findVehiclesPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: VehicleSearchParamModel,
	): Mono<PageModel<VehicleModel>>
	fun findVehicleTimeline(projectId: UUID, id: UUID): Mono<VehicleTimelineModel>
	fun findVehicleById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<VehicleModel>
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
		vehicle: VehicleModel,
	): Mono<VehicleModel>
	fun disableVehicleById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<VehicleModel>
	fun enableVehicleById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<VehicleModel>
	fun deleteVehicleById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Unit>
}
