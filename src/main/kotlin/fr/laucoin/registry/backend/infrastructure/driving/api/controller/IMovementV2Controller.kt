package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_IDS_TOO_MANY
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_COMMUNICATION_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_U
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_COMMUNICATION
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_VEHICLE
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.MOVEMENT_IDS_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SEARCH_QUERY_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SORT_MAX
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.ProjectStatusModel
import fr.laucoin.registry.backend.domain.model.VehicleStatusModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementParticipantsAndGroupsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto.MovementContentReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReasonsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PlannedMovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.GuestMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ParticipantMovementWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Purpose: REST contract of the movement endpoints under /api/v2.
 * Scope: Carries the routes, authorization rules, validation bounds and OpenAPI metadata.
 * Limits: Declares no behavior; the controller implementation delegates to the services.
 */
@Tag(name = "Movements management", description = "API for Movements-related operations")
@RequestMapping("/api/v2/projects/{projectId}/movements")
interface IMovementV2Controller {
	@Operation(
		summary = "Find Movements",
		description = "Find or get paginated Movements without content",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_R')")
	@GetMapping
	fun findMovements(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) page: Int,
		@RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) size: Int,
		@Parameter(description = "\"isCurrent\" means a movement with a REGISTERED participant still outside or a GUEST still inside")
		@RequestParam(required = false, defaultValue = "false") isCurrent: Boolean,
		@Parameter(description = "\"true\" value will be considered only if the project has REGISTRY_PROJECT_OPTION_ACTIVITY.")
		@RequestParam(required = false) hasActivity: Boolean?,
		@Parameter(description = "Visibility filter: true keeps visible elements only, false keeps hidden ones only, omitted keeps both")
		@RequestParam(required = false) isVisible: Boolean?,
		@RequestParam(required = false) type: MovementTypeEnum?,
		@Parameter(description = "Start (inclusive) of the period that must contain the element date-time")
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) from: ZonedDateTime?,
		@Parameter(description = "End (inclusive) of the period that must contain the element date-time")
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) to: ZonedDateTime?,
		@Parameter(description = "Only the movements of this activity. Considered only if the project has REGISTRY_PROJECT_OPTION_ACTIVITY.")
		@RequestParam(required = false) activityId: UUID?,
		@Parameter(description = "Sort field, prefixed with - for descending order. Supported: dateTime, type")
		@RequestParam(required = false) @Valid @Size(max = SORT_MAX, message = PARAMETER_TOO_LONG) sort: String?,
	): Mono<PageModel<MovementReaderDto>>

	@Operation(
		summary = "Find Movements contents",
		description = "Find or get content of given Movements IDs",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_R')")
	@GetMapping("/contents")
	fun findMovementsContents(
		@PathVariable projectId: UUID,
		@Parameter(description = "IDs of the movements, $MOVEMENT_IDS_MAX at most")
		@RequestParam(required = true) @Valid @Size(max = MOVEMENT_IDS_MAX, message = MOVEMENT_IDS_TOO_MANY) movementIds: List<UUID>,
		@Parameter(description = "\"isCurrent\" means a movement with a REGISTERED participant still outside or a GUEST still inside")
		@RequestParam(required = false, defaultValue = "false") isCurrent: Boolean,
	): Flux<Pair<UUID, List<MovementContentReaderDto>>>

	@Operation(
		summary = "Find Movement",
		description = "Find Movement by ID with content",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_R')")
	@GetMapping("/{id}")
	fun findMovementById(
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<MovementReaderDto>

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
		summary = "Find Movements Communications",
		description = "Find or get paginated movement communications",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_COMMUNICATION') && hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_COMMUNICATION_R')")
	@GetMapping("/{id}/communications")
	fun findMovementCommunications(
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) page: Int,
		@RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) size: Int,
		@Parameter(description = "Text matched against the communication message")
		@RequestParam(value = "q", required = false) @Valid @Size(max = SEARCH_QUERY_MAX, message = PARAMETER_TOO_LONG) query: String?,
		@Parameter(description = "Visibility filter: true keeps visible elements only, false keeps hidden ones only, omitted keeps both")
		@RequestParam(required = false) isVisible: Boolean?,
		@Parameter(description = "Start (inclusive) of the period that must contain the element date-time")
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) from: ZonedDateTime?,
		@Parameter(description = "End (inclusive) of the period that must contain the element date-time")
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) to: ZonedDateTime?,
		@Parameter(description = "Sort field, prefixed with - for descending order. Supported: dateTime, message")
		@RequestParam(required = false) @Valid @Size(max = SORT_MAX, message = PARAMETER_TOO_LONG) sort: String?,
	): Mono<PageModel<CommunicationReaderDto>>

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
	fun findParticipantsStatus(@PathVariable projectId: UUID): Mono<ProjectStatusModel>

	@Operation(
		summary = "Find vehicles status",
		description = "Return current vehicles presence status",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '${ProjectPermissionConst.REGISTRY_PROJECT_R}')")
	@GetMapping("/vehicles/status")
	fun findVehiclesStatus(@PathVariable projectId: UUID): Mono<VehicleStatusModel>

	@Operation(
		summary = "Create Movement",
		description = "Create Movement and related Content",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_C')")
	@PostMapping
	fun createMovement(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@RequestBody @Valid movement: ParticipantMovementWriterDto,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<MovementReaderDto>>

	@Operation(
		summary = "Update Movement",
		description = "Update Movement and related Content",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_U')")
	@PatchMapping("/{id}")
	fun updateMovementById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid movement: ParticipantMovementWriterDto,
		exchange: ServerWebExchange,
	): Mono<MovementReaderDto>

	@Operation(
		summary = "Create Guest Movement",
		description = "Create Movement and related Guest Content",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_C')")
	@PostMapping("/guests")
	fun createGuestsMovement(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@RequestBody @Valid movement: GuestMovementWriterDto,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<MovementReaderDto>>

	@Operation(
		summary = "Update Guest Movement",
		description = "Update Movement and related Guest Content",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_U')")
	@PatchMapping("/guests/{id}")
	fun updateGuestsMovementById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid movement: GuestMovementWriterDto,
		exchange: ServerWebExchange,
	): Mono<MovementReaderDto>

	@Operation(
		summary = "Disable Movement",
		description = "Disable Movement, it will not visible anymore in the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_U')")
	@PostMapping("/{id}/disable")
	fun disableMovementById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<MovementReaderDto>

	@Operation(
		summary = "Enable Movement",
		description = "Enable Movement, obviously it will be visible again in the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_U')")
	@PostMapping("/{id}/enable")
	fun enableMovementById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<MovementReaderDto>

	@Operation(
		summary = "Delete Movement",
		description = "Delete all Movement data.",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_D')")
	@DeleteMapping("/{id}")
	@ResponseStatus(NO_CONTENT)
	fun deleteMovementById(@PathVariable projectId: UUID, @PathVariable id: UUID): Mono<Unit>
}
