package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.annotation.RateLimited
import fr.laucoin.registry.backend.domain.constant.ApiConst.API_V2
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
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.PageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.SortedPageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.GroupWithoutMemberReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PageReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantDataExportReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ParticipantWriterDto
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
import java.util.TimeZone
import java.util.UUID

@Tag(name = "Participants management", description = "API for Participants-related operations")
@RequestMapping("$API_V2/projects/{projectId}/participants")
interface IParticipantV2Controller {
	@Operation(
		summary = "Find Participants",
		description = "Find or get paginated Participants",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_R')")
	@RateLimited(SEARCH, whenParamPresent = ["q"])
	@GetMapping
	fun findParticipants(
		@PathVariable projectId: UUID,
		@ParameterObject @Valid page: SortedPageQueryDto,
		@RequestParam(required = false) q: String?,
		@RequestParam(required = false) isMajor: Boolean?,
		@RequestParam(required = false) type: ParticipantTypeEnum?,
		@RequestParam(required = false) visible: Boolean?,
		@RequestParam(required = false) status: PresenceStatusEnum?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) dateTime: ZonedDateTime?,
	): Mono<PageReaderDto<ParticipantReaderDto>>

	@Operation(
		summary = "Find Participants",
		description = "Find or get paginated Participants",
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
	@RateLimited(SEARCH)
	@GetMapping("/search/users")
	fun searchUsers(
		@PathVariable projectId: UUID,
		@RequestParam q: String?,
	): Flux<PartialUserReaderDto>

	@Operation(
		summary = "Search Groups",
		description = "Search Groups to add Participant in it",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_METADATA_R')")
	@RateLimited(SEARCH)
	@GetMapping("/search/groups")
	fun searchGroups(
		@PathVariable projectId: UUID,
		@RequestParam q: String?,
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
		@ParameterObject @Valid page: PageQueryDto,
		@RequestParam(required = false) visible: Boolean?,
		@Parameter(description = "\"true\" value will be considered only if the project has REGISTRY_PROJECT_OPTION_ACTIVITY.")
		@RequestParam(required = false) linkedToActivity: Boolean?,
		@RequestParam(required = false) type: MovementTypeEnum?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) startDateTime: ZonedDateTime?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) endDateTime: ZonedDateTime?,
	): Mono<PageReaderDto<MovementReaderDto>>

	@Operation(
		summary = "Export Participant data",
		description = "Export all personal data held about a Participant (GDPR access/portability request), gathered on their behalf by a project administrator",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_R')")
	@RateLimited(SENSITIVE)
	@PostMapping("/{id}/data-export")
	fun exportParticipantDataById(
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<ParticipantDataExportReaderDto>

	@Operation(
		summary = "Create Participant",
		description = "Create Participant linked to the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_C')")
	@RateLimited(SENSITIVE)
	@PostMapping
	fun createParticipant(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@RequestBody @Valid participant: ParticipantWriterDto,
	): Mono<ResponseEntity<ParticipantReaderDto>>

	@Operation(
		summary = "Update Participant",
		description = "Update Participant",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_U')")
	@RateLimited(SENSITIVE)
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
		description = "Enable Participant, obviously it will be visible again in the Project",
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
	@ResponseStatus(NO_CONTENT)
	@DeleteMapping("/{id}")
	fun deleteParticipantById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<Unit>
}
