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
import org.springframework.format.annotation.DateTimeFormat
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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.UUID

@Tag(name = "Movements management", description = "API for Movements-related operations")
@RequestMapping("$API_V2/projects/{projectId}/movements")
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
		@ParameterObject @Valid page: PageQueryDto,
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
	): Mono<PageReaderDto<MovementReaderDto>>

	@Operation(
		summary = "Find Movements contents",
		description = "Find or get content of given Movements IDs",
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
	@RateLimited(SEARCH)
	@GetMapping("/search/reasons")
	fun searchReasonsAndActivities(
		@PathVariable projectId: UUID,
		@RequestParam(required = true) type: MovementTypeEnum,
		@RequestParam(required = true) contentType: ParticipantTypeEnum,
		@RequestParam q: String?,
	): Flux<MovementReasonsReaderDto>

	@Operation(
		summary = "Search Participants and/or Groups",
		description = "Search Participants and/or Groups to add in a Movement",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_METADATA_R')")
	@RateLimited(SEARCH)
	@GetMapping("/search/participants-and-groups")
	fun searchParticipantsAndGroups(
		@PathVariable projectId: UUID,
		@RequestParam(required = true) contentType: ParticipantTypeEnum,
		@RequestParam q: String?,
	): Mono<MovementParticipantsAndGroupsReaderDto>

	@Operation(
		summary = "Search Vehicles",
		description = "Search Vehicles to add in a Movement",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_METADATA_R')")
	@RateLimited(SEARCH)
	@GetMapping("/search/vehicles")
	fun searchVehicles(
		@PathVariable projectId: UUID,
		@RequestParam q: String?,
	): Flux<VehicleReaderDto>

	@Operation(
		summary = "Find Movements Communications",
		description = "Find or get paginated movement communications",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_COMMUNICATION') && hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_COMMUNICATION_R')")
	@RateLimited(SEARCH, whenParamPresent = ["q"])
	@GetMapping("/{id}/communications")
	fun findMovementCommunications(
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@ParameterObject @Valid page: PageQueryDto,
		@RequestParam(required = false) q: String?,
		@RequestParam(required = false) visible: Boolean?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) startDateTime: ZonedDateTime?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) endDateTime: ZonedDateTime?,
	): Mono<PageReaderDto<CommunicationReaderDto>>

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

	@Operation(
		summary = "Create Movement",
		description = "Create Movement and related Content",
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
		description = "Update Movement and related Content",
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
		description = "Create Movement and related Guest Content",
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
		description = "Update Movement and related Guest Content",
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
		description = "Disable Movement, it will not visible anymore in the Project",
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
		description = "Enable Movement, obviously it will be visible again in the Project",
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
	@ResponseStatus(NO_CONTENT)
	@DeleteMapping("/{id}")
	fun deleteMovementById(@PathVariable projectId: UUID, @PathVariable id: UUID): Mono<Unit>
}
