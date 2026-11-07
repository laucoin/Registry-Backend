package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import org.springdoc.core.annotations.ParameterObject
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.VehicleQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ElementMovementQueryDto
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_VEHICLE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_HISTORY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_RESTORE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_SOFT_DELETE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_U
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.VehicleTimelineReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.VehicleWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat REST des endpoints du véhicule sous /api/v2.
 * Périmètre : Porte les routes, les règles d'autorisation, les bornes de validation et les métadonnées OpenAPI.
 * Limites : Ne déclare aucun comportement ; l'implémentation du contrôleur délègue aux services.
 */
@Tag(name = "Vehicles management", description = "API for Vehicles-related operations")
@RequestMapping("/api/v2/projects/{projectId}/vehicles")
interface IVehicleV2Controller {
	@Operation(
		summary = "Find Vehicles",
		description = "Find or get paginated Vehicles",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '$REGISTRY_PROJECT_VEHICLE_R')")
	@GetMapping
	fun findVehicles(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@Valid @ParameterObject search: VehicleQueryDto,
	): Mono<PageModel<VehicleReaderDto>>

	@Operation(
		summary = "Find Vehicle",
		description = "Find Vehicle by ID",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '$REGISTRY_PROJECT_VEHICLE_R')")
	@GetMapping("/{id}")
	fun findVehicleById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<VehicleReaderDto>

	@Operation(
		summary = "Find Vehicle timeline",
		description = "Planned presence (the availability window, empty when none is set; a missing bound is open), actual outings of the Vehicle, the time spent out in seconds and the usage rate. An outing goes from an OUT movement to the next IN one and is flagged ongoing, ending now, when the Vehicle has not come back. The usage rate is the time out over the availability window, a missing bound being taken from the Project dates; it is null when no window can be determined, and the time out is then counted over all the outings",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '$REGISTRY_PROJECT_VEHICLE_HISTORY_R')")
	@GetMapping("/{id}/timeline")
	fun findVehicleTimeline(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<VehicleTimelineReaderDto>

	@Operation(
		summary = "Find Vehicle Movements",
		description = "Find or get paginated vehicle Movements",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '$REGISTRY_PROJECT_VEHICLE_HISTORY_R')")
	@GetMapping("/{id}/movements")
	fun findVehicleMovements(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@Valid @ParameterObject search: ElementMovementQueryDto,
	): Mono<PageModel<MovementReaderDto>>

	@Operation(
		summary = "Create Vehicle",
		description = "Create Vehicle linked to the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '$REGISTRY_PROJECT_VEHICLE_C')")
	@PostMapping
	fun createVehicle(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@RequestBody @Valid vehicle: VehicleWriterDto,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<VehicleReaderDto>>

	@Operation(
		summary = "Update Vehicle",
		description = "Update Vehicle",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '$REGISTRY_PROJECT_VEHICLE_U')")
	@PatchMapping("/{id}")
	fun updateVehicleById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid vehicle: VehicleWriterDto,
		exchange: ServerWebExchange,
	): Mono<VehicleReaderDto>

	@Operation(
		summary = "Disable Vehicle",
		description = "Disable Vehicle, it will not visible anymore in the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '$REGISTRY_PROJECT_VEHICLE_SOFT_DELETE')")
	@PostMapping("/{id}/disable")
	fun disableVehicleById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<VehicleReaderDto>

	@Operation(
		summary = "Enable Vehicle",
		description = "Enable Vehicle, obviously it will be visible again in the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '$REGISTRY_PROJECT_VEHICLE_RESTORE')")
	@PostMapping("/{id}/enable")
	fun enableVehicleById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<VehicleReaderDto>

	@Operation(
		summary = "Delete Vehicle",
		description = "Delete all Vehicle data.",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '$REGISTRY_PROJECT_VEHICLE_D')")
	@DeleteMapping("/{id}")
	@ResponseStatus(NO_CONTENT)
	fun deleteVehicleById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<Unit>
}
