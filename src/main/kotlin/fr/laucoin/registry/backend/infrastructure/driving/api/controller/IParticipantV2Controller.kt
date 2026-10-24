package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_HISTORY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_U
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SEARCH_QUERY_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SORT_MAX
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.GroupWithoutMemberReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ParticipantWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.TimeZone
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
 * Purpose: REST contract of the participant endpoints under /api/v2.
 * Scope: Carries the routes, authorization rules, validation bounds and OpenAPI metadata.
 * Limits: Declares no behavior; the controller implementation delegates to the services.
 */
@Tag(name = "Participants management", description = "API for Participants-related operations")
@RequestMapping("/api/v2/projects/{projectId}/participants")
interface IParticipantV2Controller {
	@Operation(
		summary = "Find Participants",
		description = "Find or get paginated Participants",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_R')")
	@GetMapping
	fun findParticipants(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) page: Int,
		@RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) size: Int,
		@Parameter(description = "Text matched against the participant first name and last name")
		@RequestParam(value = "q", required = false) @Valid @Size(max = SEARCH_QUERY_MAX, message = PARAMETER_TOO_LONG) query: String?,
		@Parameter(description = "Age filter: true keeps adults (18 or older) only, false keeps minors only, omitted keeps both")
		@RequestParam(required = false) isMajor: Boolean?,
		@RequestParam(required = false) type: ParticipantTypeEnum?,
		@Parameter(description = "Visibility filter: true keeps visible elements only, false keeps hidden ones only, omitted keeps both")
		@RequestParam(required = false) isVisible: Boolean?,
		@RequestParam(required = false) status: PresenceStatusEnum?,
		@Parameter(description = "Instant that must fall inside the element validity window")
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) at: ZonedDateTime?,
		@Parameter(description = "Account filter: true keeps participants linked to a user only, false keeps the others only, omitted keeps both")
		@RequestParam(required = false) hasUser: Boolean?,
		@Parameter(description = "On-site filter: true keeps participants that arrived and have not left yet, false keeps the others, omitted keeps both")
		@RequestParam(required = false) onSite: Boolean?,
		@Parameter(description = "Earliest birthday (inclusive), ISO date. Participants without birthday are excluded")
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE) birthdayFrom: LocalDate?,
		@Parameter(description = "Latest birthday (inclusive), ISO date. Participants without birthday are excluded")
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE) birthdayTo: LocalDate?,
		@Parameter(description = "Sort field, prefixed with - for descending order. Supported: firstName, lastName, birthday, type, startAvailability, endAvailability, status (OVERDUE, OUT, IN, UNAVAILABLE)")
		@RequestParam(required = false) @Valid @Size(max = SORT_MAX, message = PARAMETER_TOO_LONG) sort: String?,
	): Mono<PageModel<ParticipantReaderDto>>

	@Operation(
		summary = "Find Birthdays",
		description = "Find the Participants whose birthday is today",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_R')")
	@GetMapping("/birthday")
	fun findBirthdays(
		@PathVariable projectId: UUID,
	): Flux<ParticipantReaderDto>

	@Operation(
		summary = "Find Participant",
		description = "Find Participant by ID",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_R')")
	@GetMapping("/{id}")
	fun findParticipantById(
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<ParticipantReaderDto>

	@Operation(
		summary = "Search Users",
		description = "Search Users to link to a Participant",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_METADATA_R')")
	@GetMapping("/search/users")
	fun searchUsers(
		@PathVariable projectId: UUID,
		@Parameter(description = "Text matched against the user first name, last name and email")
		@RequestParam("q") @Valid @Size(max = SEARCH_QUERY_MAX, message = PARAMETER_TOO_LONG) query: String?,
	): Flux<PartialUserReaderDto>

	@Operation(
		summary = "Search Groups",
		description = "Search Groups to add Participant in it",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_METADATA_R')")
	@GetMapping("/search/groups")
	fun searchGroups(
		@PathVariable projectId: UUID,
		@Parameter(description = "Text matched against the group name (accent-insensitive substring)")
		@RequestParam("q") @Valid @Size(max = SEARCH_QUERY_MAX, message = PARAMETER_TOO_LONG) query: String?
	): Flux<GroupWithoutMemberReaderDto>

	@Operation(
		summary = "Find Participant Movements",
		description = "Find or get paginated participant Movements",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_HISTORY_R')")
	@GetMapping("/{id}/movements")
	fun findParticipantMovements(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) page: Int,
		@RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) size: Int,
		@Parameter(description = "Visibility filter: true keeps visible elements only, false keeps hidden ones only, omitted keeps both")
		@RequestParam(required = false) isVisible: Boolean?,
		@Parameter(description = "\"true\" value will be considered only if the project has REGISTRY_PROJECT_OPTION_ACTIVITY.")
		@RequestParam(required = false) hasActivity: Boolean?,
		@RequestParam(required = false) type: MovementTypeEnum?,
		@Parameter(description = "Start (inclusive) of the period that must contain the element date-time")
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) from: ZonedDateTime?,
		@Parameter(description = "End (inclusive) of the period that must contain the element date-time")
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) to: ZonedDateTime?,
		@Parameter(description = "Sort field, prefixed with - for descending order. Supported: dateTime, type")
		@RequestParam(required = false) @Valid @Size(max = SORT_MAX, message = PARAMETER_TOO_LONG) sort: String?,
	): Mono<PageModel<MovementReaderDto>>

	@Operation(
		summary = "Create Participant",
		description = "Create Participant linked to the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_C')")
	@PostMapping
	fun createParticipant(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@RequestBody @Valid participant: ParticipantWriterDto,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<ParticipantReaderDto>>

	@Operation(
		summary = "Update Participant",
		description = "Update Participant",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_U')")
	@PatchMapping("/{id}")
	fun updateParticipantById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		timeZone: TimeZone,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid participant: ParticipantWriterDto,
		exchange: ServerWebExchange,
	): Mono<ParticipantReaderDto>

	@Operation(
		summary = "Disable Participant",
		description = "Disable Participant, it will not visible anymore in the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_U')")
	@PostMapping("/{id}/disable")
	fun disableParticipantById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<ParticipantReaderDto>

	@Operation(
		summary = "Enable Participant",
		description = "Enable Participant, obviously it will be visible again in the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_D')")
	@PostMapping("/{id}/enable")
	fun enableParticipantById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<ParticipantReaderDto>

	@Operation(
		summary = "Delete Participant",
		description = "Delete all Participant data.",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_D')")
	@DeleteMapping("/{id}")
	@ResponseStatus(NO_CONTENT)
	fun deleteParticipantById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<Unit>
}
