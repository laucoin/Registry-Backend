package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.model.VehicleTimelineModel
import fr.laucoin.registry.backend.domain.service.core.IVehicleService
import fr.laucoin.registry.backend.domain.service.usecase.IVehicleUseCase
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * Objectif : Point d'entrée des véhicules pour la couche REST.
 * Périmètre : Délègue chaque opération au service Vehicle sans logique propre.
 * Limites : Ne porte aucune règle métier ; elles restent dans le service.
 */
@Service
class VehicleUseCase(private val service: IVehicleService) : IVehicleUseCase {
	override fun findVehiclesPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: VehicleSearchParamModel,
	): Mono<PageModel<VehicleModel>> {
		return service.findVehiclesPage(projectId, pageable, searchParams)
	}

	override fun findVehicleTimeline(projectId: UUID, id: UUID): Mono<VehicleTimelineModel> {
		return service.findVehicleTimeline(projectId, id)
	}

	override fun findVehicleById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<VehicleModel> {
		return service.findVehicleById(projectId, id, isVisible)
	}

	override fun findVehicleMovementsPage(
		projectId: UUID,
		id: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
	): Mono<PageModel<MovementModel>> {
		return service.findVehicleMovementsPage(projectId, id, pageable, searchParams)
	}

	override fun createVehicle(currentUser: CurrentUserModel, vehicle: VehicleModel): Mono<VehicleModel> {
		return service.createVehicle(currentUser, vehicle)
	}

	override fun updateVehicleById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		vehicle: VehicleModel,
	): Mono<VehicleModel> {
		return service.updateVehicleById(currentUser, projectId, id, vehicle)
	}

	override fun disableVehicleById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<VehicleModel> {
		return service.disableVehicleById(currentUser, projectId, id)
	}

	override fun enableVehicleById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<VehicleModel> {
		return service.enableVehicleById(currentUser, projectId, id)
	}

	override fun deleteVehicleById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Unit> {
		return service.deleteVehicleById(currentUser, projectId, id)
	}
}
