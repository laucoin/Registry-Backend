package fr.laucoin.registry.backend.infrastructure.out.api.controller

import fr.laucoin.registry.backend.domain.annotation.RateLimited
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
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

@Deprecated(
	"API v1 has no remaining Registry-Frontend consumer and is scheduled for removal; use the /api/v2 contract.",
	level = DeprecationLevel.WARNING,
)
@Tag(name = "Participants management (v1, deprecated)", description = "API for Participants-related operations — deprecated, scheduled for removal; use /api/v2")
@RequestMapping("/api/v1/projects/{projectId}/participants")
interface IParticipantV1Controller {
	@Operation(
		summary = "Find Participants",
		description = "Find or get paginated Participants",
		deprecated = true,
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_R')")
	@RateLimited(SEARCH, whenParamPresent = ["textSearched"])
	@GetMapping
	fun findParticipants(
		@PathVariable projectId: UUID,
		@RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) pageNumber: Int,
		@RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) pageSize: Int,
		@RequestParam(required = false) textSearched: String?,
		@RequestParam(required = false) isMajor: Boolean?,
		@RequestParam(required = false) typeSearched: ParticipantTypeEnum?,
		@RequestParam(required = false) visibilitySearched: Boolean?,
		@RequestParam(required = false) statusSearched: PresenceStatusEnum?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) dateTimeSearched: ZonedDateTime?,
	): Mono<PageModel<ParticipantReaderDto>>

	@Operation(
		summary = "Find Participants",
		description = "Find or get paginated Participants",
		deprecated = true,
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_R')")
	@GetMapping("/birthday")
	fun findBirthdays(
		@PathVariable projectId: UUID,
	): Flux<ParticipantReaderDto>

	@Operation(
		summary = "Find Participant",
		description = "Find Participant by ID",
		deprecated = true,
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
		deprecated = true,
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_METADATA_R')")
	@RateLimited(SEARCH, whenParamPresent = ["textSearched"])
	@GetMapping("/search/users")
	fun searchUsers(
		@PathVariable projectId: UUID,
		@RequestParam textSearched: String?,
	): Flux<PartialUserReaderDto>

	@Operation(
		summary = "Search Groups",
		description = "Search Groups to add Participant in it",
		deprecated = true,
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_METADATA_R')")
	@RateLimited(SEARCH, whenParamPresent = ["textSearched"])
	@GetMapping("/search/groups")
	fun searchGroups(
		@PathVariable projectId: UUID,
		@RequestParam textSearched: String?
	): Flux<GroupWithoutMemberReaderDto>

	@Operation(
		summary = "Find Participant Movements",
		description = "Find or get paginated participant Movements",
		deprecated = true,
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_HISTORY_R')")
	@GetMapping("/{id}/movements")
	fun findParticipantMovements(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) pageNumber: Int,
		@RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) pageSize: Int,
		@RequestParam(required = false) visibilitySearched: Boolean?,
		@Parameter(description = "\"true\" value will be considered only if the project has REGISTRY_PROJECT_OPTION_ACTIVITY.")
		@RequestParam(required = false) linkedToActivity: Boolean?,
		@RequestParam(required = false) typeSearched: MovementTypeEnum?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) startDateTimeSearched: ZonedDateTime?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) endDateTimeSearched: ZonedDateTime?,
	): Mono<PageModel<MovementReaderDto>>

	@Operation(
		summary = "Create Participant",
		description = "Create Participant linked to the Project",
		deprecated = true,
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
		description = "Update Participant",
		deprecated = true,
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
		description = "Disable Participant, it will not visible anymore in the Project",
		deprecated = true,
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_U')")
	@RateLimited(SENSITIVE)
	@PatchMapping("/{id}/disable")
	fun disableParticipantById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<ParticipantReaderDto>

	@Operation(
		summary = "Enable Participant",
		description = "Enable Participant, obviously it will be visible again in the Project",
		deprecated = true,
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_U')")
	@RateLimited(SENSITIVE)
	@PatchMapping("/{id}/enable")
	fun enableParticipantById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<ParticipantReaderDto>

	@Operation(
		summary = "Delete Participant",
		description = "Delete all Participant data.",
		deprecated = true,
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
