package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.service.usecase.IMovementUseCase
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IMovementStatusV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectStatusReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.VehicleStatusReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ProjectStatusReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.VehicleStatusReaderDtoMapper
import java.util.UUID
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * Objectif : Adaptateur REST des statuts de présence des participants et des véhicules.
 * Périmètre : Mappe les modèles de statut en DTO et délègue au cas d'usage des mouvements.
 * Limites : Ne porte aucune règle métier ; l'autorisation est déclarée sur le contrat.
 */
@RestController
class MovementStatusV2Controller(
	private val service: IMovementUseCase,
	private val projectStatusMapper: ProjectStatusReaderDtoMapper,
	private val vehicleStatusMapper: VehicleStatusReaderDtoMapper,
): IMovementStatusV2Controller {
	override fun findParticipantsStatus(projectId: UUID): Mono<ProjectStatusReaderDto> {
		return service.findParticipantsStatus(projectId).map(projectStatusMapper::toDto)
	}

	override fun findVehiclesStatus(projectId: UUID): Mono<VehicleStatusReaderDto> {
		return service.findVehiclesStatus(projectId).map(vehicleStatusMapper::toDto)
	}
}
