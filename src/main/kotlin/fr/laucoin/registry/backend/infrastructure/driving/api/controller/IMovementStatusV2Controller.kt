package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_VEHICLE
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectStatusReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.VehicleStatusReaderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat REST des statuts de présence des participants et des véhicules.
 * Périmètre : Porte les deux routes de statut, leurs autorisations et leurs métadonnées OpenAPI.
 * Limites : Ne déclare aucun comportement ; l'implémentation du contrôleur délègue au cas d'usage.
 */
@Tag(name = "Movements management", description = "API for Movements-related operations")
@RequestMapping("/api/v2/projects/{projectId}/movements")
interface IMovementStatusV2Controller {
	@Operation(
		summary = "Find participants status",
		description = "Return current major and minor status presence status",
	)
	@PreAuthorize("hasPermission(#projectId, '${ProjectPermissionConst.REGISTRY_PROJECT_R}')")
	@GetMapping("/participants/status")
	fun findParticipantsStatus(@PathVariable projectId: UUID): Mono<ProjectStatusReaderDto>

	@Operation(
		summary = "Find vehicles status",
		description = "Return current vehicles presence status",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '${ProjectPermissionConst.REGISTRY_PROJECT_R}')")
	@GetMapping("/vehicles/status")
	fun findVehiclesStatus(@PathVariable projectId: UUID): Mono<VehicleStatusReaderDto>
}
