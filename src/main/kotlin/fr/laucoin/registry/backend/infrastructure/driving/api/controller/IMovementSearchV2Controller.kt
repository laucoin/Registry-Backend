package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_VEHICLE
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SEARCH_QUERY_MAX
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementParticipantsAndGroupsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReasonsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PlannedMovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectStatusReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.VehicleStatusReaderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat REST de la recherche, des mouvements planifiés et des statuts de présence sous /api/v2.
 * Périmètre : Porte les routes, les règles d'autorisation, les bornes de validation et les métadonnées OpenAPI.
 * Limites : Ne déclare aucun comportement ; l'implémentation du contrôleur délègue aux services.
 */
@Tag(name = "Movements management", description = "API for Movements-related operations")
@RequestMapping("/api/v2/projects/{projectId}/movements")
interface IMovementSearchV2Controller {
	@Operation(
		summary = "Search Reasons (and Activity as reason)",
		description = "Search Reasons (and Activity as reason) to add in a Movement",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_METADATA_R')")
	@GetMapping("/search/reasons")
	fun searchReasonsAndActivities(
		@PathVariable projectId: UUID,
		@RequestParam(required = true) type: MovementTypeEnum,
		@RequestParam(required = true) contentType: ParticipantTypeEnum,
		@Parameter(description = "Text matched against the name and description of the activity")
		@RequestParam("q") @Valid @Size(max = SEARCH_QUERY_MAX, message = PARAMETER_TOO_LONG) query: String?,
	): Flux<MovementReasonsReaderDto>

	@Operation(
		summary = "Search Participants and/or Groups",
		description = "Search Participants and/or Groups to add in a Movement",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_METADATA_R')")
	@GetMapping("/search/participants-and-groups")
	fun searchParticipantsAndGroups(
		@PathVariable projectId: UUID,
		@RequestParam(required = true) contentType: ParticipantTypeEnum,
		@Parameter(description = "Text matched against the participant first name and last name, or the group name")
		@RequestParam("q") @Valid @Size(max = SEARCH_QUERY_MAX, message = PARAMETER_TOO_LONG) query: String?,
		@Parameter(description = "Date-time of the movement: participants must be present at this instant")
		@RequestParam(required = true)
		@DateTimeFormat(iso = DATE_TIME) at: ZonedDateTime,
		@Parameter(description = "When true, returns every visible participant of the project regardless of the date")
		@RequestParam(required = false, defaultValue = "false") all: Boolean,
	): Mono<MovementParticipantsAndGroupsReaderDto>

	@Operation(
		summary = "Search Vehicles",
		description = "Search Vehicles to add in a Movement",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_METADATA_R')")
	@GetMapping("/search/vehicles")
	fun searchVehicles(
		@PathVariable projectId: UUID,
		@Parameter(description = "Text matched against the vehicle license plate, brand and model")
		@RequestParam("q") @Valid @Size(max = SEARCH_QUERY_MAX, message = PARAMETER_TOO_LONG) query: String?
	): Flux<VehicleReaderDto>

	@Operation(
		summary = "Find planned arrivals",
		description = "Find movement drafts (without date-time) of the participants planned to arrive on a day, with their planned date-time. Participants with their own planned dates come one per draft; the others are grouped by group, with the group name as pool name",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_C')")
	@GetMapping("/planned/arrivals")
	fun findPlannedArrivals(
		@PathVariable projectId: UUID,
		@Parameter(description = "Planned day (ISO date), today when omitted")
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE) date: LocalDate?,
	): Flux<PlannedMovementReaderDto>

	@Operation(
		summary = "Find planned departures",
		description = "Find definitive-departure movement drafts (without date-time) of the participants planned to leave on a day, with their planned date-time. Participants with their own planned dates come one per draft; the others are grouped by group, with the group name as pool name",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_C')")
	@GetMapping("/planned/departures")
	fun findPlannedDepartures(
		@PathVariable projectId: UUID,
		@Parameter(description = "Planned day (ISO date), today when omitted")
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE) date: LocalDate?,
	): Flux<PlannedMovementReaderDto>

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
