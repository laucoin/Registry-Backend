package fr.laucoin.registry.backend.infrastructure.out.api.controller

import fr.laucoin.registry.backend.domain.annotation.RateLimited
import fr.laucoin.registry.backend.domain.constant.ApiConst.DEFAULT_COLLECTION_LIMIT_PARAM
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_HISTORY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_U
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SEARCH
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SENSITIVE
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.DueTodayReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.GroupWithoutMemberReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ParticipantWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
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
import java.util.TimeZone
import java.util.UUID

/**
 * API v2 Participants contract (ADR 017):
 * - list grammar: `page`/`size`/`sort=field,-other`/`q`/typed filters (§5)
 * - state transitions as explicit `POST` actions, no value-in-path (§3)
 * - eligibility sub-collections named for their relationship (§4)
 */
@Tag(name = "Participants management", description = "API for Participants-related operations")
@RequestMapping("/api/v2/projects/{projectId}/participants")
interface IParticipantV2Controller {
	@Operation(
		summary = "Find Participants",
		description = "Paginated Participants",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_R')")
	@RateLimited(SEARCH, whenParamPresent = ["q"])
	@GetMapping
	fun findParticipants(
		@PathVariable projectId: UUID,
		@RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) page: Int,
		@RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) size: Int,
		@RequestParam(required = false) sort: List<String>?,
		@RequestParam(required = false) q: String?,
		@RequestParam(required = false) isMajor: Boolean?,
		@RequestParam(required = false) type: ParticipantTypeEnum?,
		@RequestParam(required = false) visible: Boolean?,
		@RequestParam(required = false) status: PresenceStatusEnum?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) dateTime: ZonedDateTime?,
	): Mono<PageModel<ParticipantReaderDto>>

	@Operation(
		summary = "Find birthday Participants",
		description = "Participants whose birthday is today; capped at `limit` rows",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_R')")
	@GetMapping("/birthdays")
	fun findBirthdays(
		@PathVariable projectId: UUID,
		@RequestParam(defaultValue = DEFAULT_COLLECTION_LIMIT_PARAM) @Valid @Min(
			1,
			message = PAGE_SIZE_IS_LOWER_THAN_ONE
		) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) limit: Int,
	): Flux<ParticipantReaderDto>

	@Operation(
		summary = "Find participants arriving today",
		description = "Participants scheduled to arrive today (effective availability starts today, participant window over group's) who are not yet on site (home dashboard); capped at `limit` rows",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_R')")
	@GetMapping("/arriving-today")
	fun findArrivingToday(
		@PathVariable projectId: UUID,
		@RequestParam(defaultValue = DEFAULT_COLLECTION_LIMIT_PARAM) @Valid @Min(
			1,
			message = PAGE_SIZE_IS_LOWER_THAN_ONE
		) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) limit: Int,
	): Flux<ParticipantReaderDto>

	@Operation(
		summary = "Find participants departing today",
		description = "Participants scheduled to leave today (effective availability ends today, participant window over group's) who are currently on site (home dashboard); capped at `limit` rows",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_R')")
	@GetMapping("/departing-today")
	fun findDepartingToday(
		@PathVariable projectId: UUID,
		@RequestParam(defaultValue = DEFAULT_COLLECTION_LIMIT_PARAM) @Valid @Min(
			1,
			message = PAGE_SIZE_IS_LOWER_THAN_ONE
		) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) limit: Int,
	): Flux<ParticipantReaderDto>

	@Operation(
		summary = "Find who is due to arrive today",
		description = "Participants scheduled to arrive today AND groups whose own window opens today; both sides are queried concurrently. Capped at `limit` rows per side",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_R') && hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_R')")
	@GetMapping("/arrivals-today")
	fun findArrivalsToday(
		@PathVariable projectId: UUID,
		@RequestParam(defaultValue = DEFAULT_COLLECTION_LIMIT_PARAM) @Valid @Min(
			1,
			message = PAGE_SIZE_IS_LOWER_THAN_ONE
		) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) limit: Int,
	): Mono<DueTodayReaderDto>

	@Operation(
		summary = "Find who is due to leave today",
		description = "Participants scheduled to leave today AND groups whose own window closes today; both sides are queried concurrently. Capped at `limit` rows per side",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_R') && hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_R')")
	@GetMapping("/departures-today")
	fun findDeparturesToday(
		@PathVariable projectId: UUID,
		@RequestParam(defaultValue = DEFAULT_COLLECTION_LIMIT_PARAM) @Valid @Min(
			1,
			message = PAGE_SIZE_IS_LOWER_THAN_ONE
		) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) limit: Int,
	): Mono<DueTodayReaderDto>

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
		summary = "Find linkable Users",
		description = "Users eligible to be linked to a Participant of this Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_METADATA_R')")
	@RateLimited(SEARCH)
	@GetMapping("/linkable-users")
	fun findLinkableUsers(
		@PathVariable projectId: UUID,
		@RequestParam(required = false) q: String?,
	): Flux<PartialUserReaderDto>

	@Operation(
		summary = "Find linkable Groups",
		description = "Groups eligible to receive a Participant of this Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_METADATA_R')")
	@RateLimited(SEARCH)
	@GetMapping("/linkable-groups")
	fun findLinkableGroups(
		@PathVariable projectId: UUID,
		@RequestParam(required = false) q: String?,
	): Flux<GroupWithoutMemberReaderDto>

	@Operation(
		summary = "Find Participant Movements",
		description = "Paginated movement history of a Participant",
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
		@RequestParam(required = false) visible: Boolean?,
		@Parameter(description = "\"true\" value will be considered only if the project has REGISTRY_PROJECT_OPTION_ACTIVITY.")
		@RequestParam(required = false) linkedToActivity: Boolean?,
		@RequestParam(required = false) type: MovementTypeEnum?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) startDateTime: ZonedDateTime?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) endDateTime: ZonedDateTime?,
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
	): Mono<ParticipantReaderDto>

	@Operation(
		summary = "Update Participant",
		description = "Update a Participant's editable fields",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_U')")
	@PatchMapping("/{id}")
	fun updateParticipantById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		timeZone: TimeZone,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid participant: ParticipantWriterDto,
	): Mono<ParticipantReaderDto>

	@Operation(
		summary = "Disable Participant",
		description = "Disable Participant, it will not be visible anymore in the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_U')")
	@RateLimited(SENSITIVE)
	@PostMapping("/{id}/disable")
	fun disableParticipantById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<ParticipantReaderDto>

	@Operation(
		summary = "Enable Participant",
		description = "Enable Participant, it will be visible again in the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_U')")
	@RateLimited(SENSITIVE)
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
	@RateLimited(SENSITIVE)
	@DeleteMapping("/{id}")
	fun deleteParticipantById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<Unit>
}
