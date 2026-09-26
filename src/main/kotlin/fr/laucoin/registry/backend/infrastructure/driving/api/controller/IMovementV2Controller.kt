package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.annotation.RateLimited
import fr.laucoin.registry.backend.domain.constant.ApiConst.API_V2
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_COMMUNICATION_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_U
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_COMMUNICATION
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_VEHICLE
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SEARCH
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SENSITIVE
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.DateTimeRangeQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.PageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementParticipantsAndGroupsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto.MovementContentReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReasonsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PageReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectStatusReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.VehicleStatusReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.GuestMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ParticipantMovementWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Tag(name = "Movements management", description = "API for Movements-related operations")
@RequestMapping("$API_V2/projects/{projectId}/movements")
interface IMovementV2Controller {
	@Operation(
		summary = "Find Movements",
		description = """
			Search and list the Project's Movements (without their content), with pagination. Combine `currentMovements`
			(only ongoing outings/visits still open), `linkedToActivity`, `visible`, `type` (IN / OUT) and a `startDateTime`/`endDateTime`
			range to narrow the results.
		""",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_R')")
	@GetMapping
	fun findMovements(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@ParameterObject @Valid page: PageQueryDto,
		@Parameter(description = "\"currentMovements\" means a movement with a REGISTERED participant still outside or a GUEST still inside")
		@RequestParam(required = false, defaultValue = "false") currentMovements: Boolean,
		@Parameter(description = "\"true\" value will be considered only if the project has REGISTRY_PROJECT_OPTION_ACTIVITY.")
		@RequestParam(required = false) linkedToActivity: Boolean?,
		@RequestParam(required = false) visible: Boolean?,
		@RequestParam(required = false) type: MovementTypeEnum?,
		@ParameterObject dateTimeRange: DateTimeRangeQueryDto,
	): Mono<PageReaderDto<MovementReaderDto>>

	@Operation(
		summary = "Find Movements contents",
		description = """
			Batch-fetch the content (who/what moved) of several Movements at once, given their IDs.
			Returns one entry per requested Movement ID paired with its list of contents.
		""",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_R')")
	@GetMapping("/contents")
	fun findMovementsContents(
		@PathVariable projectId: UUID,
		@RequestParam(required = true) movementIds: List<UUID>,
		@Parameter(description = "\"currentMovements\" means a movement with a REGISTERED participant still outside or a GUEST still inside")
		@RequestParam(required = false, defaultValue = "false") currentMovements: Boolean,
	): Flux<Pair<UUID, List<MovementContentReaderDto>>>

	@Operation(
		summary = "Find Movement",
		description = "Get a single Movement of the Project by its ID, with its full content (Participants, Groups, Guests or Vehicle involved).",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_R')")
	@GetMapping("/{id}")
	fun findMovementById(
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<MovementReaderDto>

	@Operation(
		summary = "Search Reasons (and Activity as reason)",
		description = """
			Search the Movement reasons (and, when relevant, Activities usable as a reason) compatible with the given
			`type` (IN / OUT) and `contentType` (REGISTERED / GUEST), to attach to a new Movement.
		""",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_METADATA_R')")
	@RateLimited(SEARCH)
	@GetMapping("/search/reasons")
	fun searchReasonsAndActivities(
		@PathVariable projectId: UUID,
		@RequestParam(required = true) type: MovementTypeEnum,
		@RequestParam(required = true) contentType: ParticipantTypeEnum,
		@RequestParam(name = "q", required = false) query: String?,
	): Flux<MovementReasonsReaderDto>

	@Operation(
		summary = "Search Participants and/or Groups",
		description = "Search Participants and/or Groups of the given `contentType` (REGISTERED / GUEST), to add as content of a new Movement.",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_METADATA_R')")
	@RateLimited(SEARCH)
	@GetMapping("/search/participants-and-groups")
	fun searchParticipantsAndGroups(
		@PathVariable projectId: UUID,
		@RequestParam(required = true) contentType: ParticipantTypeEnum,
		@RequestParam(name = "q", required = false) query: String?,
	): Mono<MovementParticipantsAndGroupsReaderDto>

	@Operation(
		summary = "Search Vehicles",
		description = "Search Vehicles of the Project, to attach one to a new Movement.",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_METADATA_R')")
	@RateLimited(SEARCH)
	@GetMapping("/search/vehicles")
	fun searchVehicles(
		@PathVariable projectId: UUID,
		@RequestParam(name = "q", required = false) query: String?,
	): Flux<VehicleReaderDto>

	@Operation(
		summary = "Find Movements Communications",
		description = """
			List, paginated, the Communications posted on this Movement, optionally filtered by free-text search,
			visibility and a date/time range.
		""",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_COMMUNICATION') && hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_COMMUNICATION_R')")
	@RateLimited(SEARCH, whenParamPresent = ["q"])
	@GetMapping("/{id}/communications")
	fun findMovementCommunications(
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@ParameterObject @Valid page: PageQueryDto,
		@RequestParam(name = "q", required = false) query: String?,
		@RequestParam(required = false) visible: Boolean?,
		@ParameterObject dateTimeRange: DateTimeRangeQueryDto,
	): Mono<PageReaderDto<CommunicationReaderDto>>

	@Operation(
		summary = "Find participants status",
		description = "Dashboard widget: current count of major/minor Participants who are IN, OUT or UNAVAILABLE.",
	)
	@PreAuthorize("hasPermission(#projectId, '${ProjectPermissionConst.REGISTRY_PROJECT_R}')")
	@GetMapping("/participants/status")
	fun findParticipantsStatus(@PathVariable projectId: UUID): Mono<ProjectStatusReaderDto>

	@Operation(
		summary = "Find vehicles status",
		description = "Dashboard widget: current count of Vehicles that are IN, OUT or UNAVAILABLE.",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '${ProjectPermissionConst.REGISTRY_PROJECT_R}')")
	@GetMapping("/vehicles/status")
	fun findVehiclesStatus(@PathVariable projectId: UUID): Mono<VehicleStatusReaderDto>

	@Operation(
		summary = "Create Movement",
		description = """
			Record a new IN/OUT Movement for one or more Participants and/or Groups, each entry optionally linked to a
			Vehicle and/or a carpool name (`poolName`). Exactly one of `reason` or `activityId` may be set, and both must stay
			compatible with the Movement `type` and the Participant type.
		""",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_C')")
	@RateLimited(SENSITIVE)
	@PostMapping
	fun createMovement(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@RequestBody @Valid movement: ParticipantMovementWriterDto,
	): Mono<ResponseEntity<MovementReaderDto>>

	@Operation(
		summary = "Update Movement",
		description = "Update an existing REGISTERED-content Movement: its date/time, type, reason/Activity and content (same shape as creation).",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_U')")
	@RateLimited(SENSITIVE)
	@PatchMapping("/{id}")
	fun updateMovementById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid movement: ParticipantMovementWriterDto,
	): Mono<MovementReaderDto>

	@Operation(
		summary = "Create Guest Movement",
		description = """
			Record a new IN/OUT Movement for one or more Guests. Existing Guests can be referenced by `id`, or created
			inline by providing their first name, last name and birthday.
		""",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_C')")
	@RateLimited(SENSITIVE)
	@PostMapping("/guests")
	fun createGuestsMovement(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@RequestBody @Valid movement: GuestMovementWriterDto,
	): Mono<ResponseEntity<MovementReaderDto>>

	@Operation(
		summary = "Update Guest Movement",
		description = "Update an existing GUEST-content Movement: its date/time, type, reason and guest content (same shape as creation).",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_U')")
	@RateLimited(SENSITIVE)
	@PatchMapping("/guests/{id}")
	fun updateGuestsMovementById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid movement: GuestMovementWriterDto,
	): Mono<MovementReaderDto>

	@Operation(
		summary = "Disable Movement",
		description = "Soft-delete the Movement: it is kept (with its Communications) but hidden from the Project going forward.",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_U')")
	@RateLimited(SENSITIVE)
	@PostMapping("/{id}/disable")
	fun disableMovementById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<MovementReaderDto>

	@Operation(
		summary = "Enable Movement",
		description = "Reverse a disable: the Movement becomes visible in the Project again.",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_U')")
	@RateLimited(SENSITIVE)
	@PostMapping("/{id}/enable")
	fun enableMovementById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<MovementReaderDto>

	@Operation(
		summary = "Delete Movement",
		description = """
			Permanently delete the Movement and all its data, including its Communications. This cannot be undone;
			prefer disabling it if it may be needed again.
		""",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_D')")
	@RateLimited(SENSITIVE)
	@ResponseStatus(NO_CONTENT)
	@DeleteMapping("/{id}")
	fun deleteMovementById(@PathVariable projectId: UUID, @PathVariable id: UUID): Mono<Unit>
}
