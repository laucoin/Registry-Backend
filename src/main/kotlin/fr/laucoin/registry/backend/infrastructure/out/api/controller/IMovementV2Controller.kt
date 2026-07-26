package fr.laucoin.registry.backend.infrastructure.out.api.controller

import fr.laucoin.registry.backend.domain.annotation.RateLimited
import fr.laucoin.registry.backend.domain.constant.ApiConst.DEFAULT_COLLECTION_LIMIT_PARAM
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_IDS_SIZE_IS_UPPER_THAN_MAX
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_COMMUNICATION_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_U
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ACTIVITY
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_COMMUNICATION
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_VEHICLE
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SEARCH
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SENSITIVE
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.ProjectStatusModel
import fr.laucoin.registry.backend.domain.model.VehicleStatusModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementContentsReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementParticipantsAndGroupsReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReasonsReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.GuestMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ParticipantMovementWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.UUID

/**
 * API v2 Movements contract (ADR 017):
 * - list grammar: `page`/`size`/`sort=field,-other`/typed filters (§5)
 * - state transitions as explicit `POST` actions, no value-in-path (§3)
 * - eligibility sub-collections named for their relationship (§4)
 */
@Tag(name = "Movements management", description = "API for Movements-related operations")
@RequestMapping("/api/v2/projects/{projectId}/movements")
interface IMovementV2Controller {
	@Operation(
		summary = "Find Movements",
		description = "Paginated Movements without content",
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
		@RequestParam(required = false) sort: List<String>?,
		@Parameter(description = "\"currentMovements\" means a movement with a REGISTERED participant still outside or a GUEST still inside")
		@RequestParam(required = false, defaultValue = "false") currentMovements: Boolean,
		@Parameter(description = "\"true\" value will be considered only if the project has REGISTRY_PROJECT_OPTION_ACTIVITY.")
		@RequestParam(required = false) linkedToActivity: Boolean?,
		@RequestParam(required = false) visible: Boolean?,
		@RequestParam(required = false) type: MovementTypeEnum?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) startDateTime: ZonedDateTime?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) endDateTime: ZonedDateTime?,
	): Mono<PageModel<MovementReaderDto>>

	@Operation(
		summary = "Find Movements contents",
		description = "Find or get content of given Movements IDs (at most 200 IDs per request)",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_R')")
	@GetMapping("/contents")
	fun findMovementsContents(
		@PathVariable projectId: UUID,
		@RequestParam(required = true) @Valid @Size(
			max = 200,
			message = MOVEMENT_IDS_SIZE_IS_UPPER_THAN_MAX
		) movementIds: List<UUID>,
		@Parameter(description = "\"currentMovements\" means a movement with a REGISTERED participant still outside or a GUEST still inside")
		@RequestParam(required = false, defaultValue = "false") currentMovements: Boolean,
	): Flux<MovementContentsReaderDto>

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
		summary = "Find Reasons (and Activity as reason)",
		description = "Reasons (and Activities as reason) eligible for the Movement form",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_METADATA_R')")
	@RateLimited(SEARCH)
	@GetMapping("/reasons")
	fun findReasons(
		@PathVariable projectId: UUID,
		@RequestParam(required = true) type: MovementTypeEnum,
		@RequestParam(required = true) contentType: ParticipantTypeEnum,
		@RequestParam(required = false) q: String?,
	): Flux<MovementReasonsReaderDto>

	@Operation(
		summary = "Find eligible Participants and/or Groups",
		description = "Participants and/or Groups eligible for a Movement of this Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_METADATA_R')")
	@RateLimited(SEARCH)
	@GetMapping("/eligible-participants-and-groups")
	fun findEligibleParticipantsAndGroups(
		@PathVariable projectId: UUID,
		@RequestParam(required = true) contentType: ParticipantTypeEnum,
		@RequestParam(required = false) q: String?,
	): Mono<MovementParticipantsAndGroupsReaderDto>

	@Operation(
		summary = "Find eligible Vehicles",
		description = "Vehicles eligible for a Movement of this Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_METADATA_R')")
	@RateLimited(SEARCH)
	@GetMapping("/eligible-vehicles")
	fun findEligibleVehicles(
		@PathVariable projectId: UUID,
		@RequestParam(required = false) q: String?,
	): Flux<VehicleReaderDto>

	@Operation(
		summary = "Find Movements Communications",
		description = "Paginated communications of a Movement",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_COMMUNICATION') && hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_COMMUNICATION_R')")
	@RateLimited(SEARCH, whenParamPresent = ["q"])
	@GetMapping("/{id}/communications")
	fun findMovementCommunications(
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) page: Int,
		@RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) size: Int,
		@RequestParam(required = false) q: String?,
		@RequestParam(required = false) visible: Boolean?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) startDateTime: ZonedDateTime?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) endDateTime: ZonedDateTime?,
	): Mono<PageModel<CommunicationReaderDto>>

	@Operation(
		summary = "Find participants status",
		description = "Return current major and minor presence status",
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
		summary = "Find ongoing activity outings",
		description = "Activity-linked outings still in progress (latest movement per activity is an OUT), each with the date of its most recent communication for a live chronometer (project overview dashboard); capped at `limit` rows",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_R')")
	@GetMapping("/activities/ongoing")
	fun findOngoingActivities(
		@PathVariable projectId: UUID,
		@RequestParam(defaultValue = DEFAULT_COLLECTION_LIMIT_PARAM) @Valid @Min(
			1,
			message = PAGE_SIZE_IS_LOWER_THAN_ONE
		) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) limit: Int,
	): Flux<MovementReaderDto>

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
	): Mono<MovementReaderDto>

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
	): Mono<MovementReaderDto>

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
	): Mono<MovementReaderDto>

	@Operation(
		summary = "Disable Movement",
		description = "Disable Movement, it will not be visible anymore in the Project",
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
		description = "Enable Movement, it will be visible again in the Project",
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
		description = "Delete all Movement data.",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_D')")
	@RateLimited(SENSITIVE)
	@DeleteMapping("/{id}")
	fun deleteMovementById(@PathVariable projectId: UUID, @PathVariable id: UUID): Mono<Unit>
}
