package fr.laucoin.registry.backend.infrastructure.out.api.controller

import fr.laucoin.registry.backend.domain.annotation.RateLimited
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_U
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SEARCH
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SENSITIVE
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.AddedGroupMembersReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.GroupReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.GroupWithoutMemberReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.GroupMembersWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.GroupWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.UUID

/**
 * API v2 Groups contract (ADR 017):
 * - list grammar: `page`/`size`/`sort=field,other`/`direction=ASC|DESC`/`q`/typed filters (§5)
 * - state transitions as explicit `POST` actions, no value-in-path (§3)
 * - eligibility sub-collections named for their relationship (§4)
 */
@Tag(name = "Participant's groups management", description = "API for Group-related operations")
@RequestMapping("/api/v2/projects/{projectId}/groups")
interface IGroupV2Controller {
	@Operation(
		summary = "Find Groups",
		description = "Paginated Groups",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_R')")
	@RateLimited(SEARCH, whenParamPresent = ["q"])
	@GetMapping
	fun findGroups(
		@PathVariable projectId: UUID,
		@RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) page: Int,
		@RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) size: Int,
		@RequestParam(required = false) sort: List<String>?,
		@RequestParam(required = false, defaultValue = "ASC") direction: String,
		@RequestParam(required = false) q: String?,
		@RequestParam(required = false) visible: Boolean?,
		@RequestParam(required = false) presence: Boolean?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) dateTime: ZonedDateTime?,
	): Mono<PageModel<GroupWithoutMemberReaderDto>>

	@Operation(
		summary = "Find Group",
		description = "Find Group by ID",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_R')")
	@GetMapping("/{id}")
	fun findGroupById(
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<GroupReaderDto>

	@Operation(
		summary = "Find Group Members",
		description = "Paginated Members of a Group",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_R')")
	@RateLimited(SEARCH, whenParamPresent = ["q"])
	@GetMapping("/{id}/members")
	fun findGroupMembersByGroupId(
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) page: Int,
		@RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) size: Int,
		@RequestParam(required = false) q: String?,
		@RequestParam(required = false) isMajor: Boolean?,
		@RequestParam(required = false) type: ParticipantTypeEnum?,
		@RequestParam(required = false) visible: Boolean?,
		@RequestParam(required = false) status: PresenceStatusEnum?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) dateTime: ZonedDateTime?,
	): Mono<PageModel<ParticipantReaderDto>>

	@Operation(
		summary = "Find assignable Participants",
		description = "Participants eligible to be added to a Group of this Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_METADATA_R')")
	@RateLimited(SEARCH)
	@GetMapping("/assignable-participants")
	fun findAssignableParticipants(
		@PathVariable projectId: UUID,
		@RequestParam(required = false) q: String?,
	): Flux<ParticipantReaderDto>

	@Operation(
		summary = "Create Group",
		description = "Create Group and related Group Content",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_C')")
	@PostMapping
	fun createGroup(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@RequestBody @Valid group: GroupWriterDto,
	): Mono<GroupWithoutMemberReaderDto>

	@Operation(
		summary = "Update Group",
		description = "Update a Group's editable fields",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_U')")
	@PatchMapping("/{id}")
	fun updateGroupById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid group: GroupWriterDto,
	): Mono<GroupWithoutMemberReaderDto>

	@Operation(
		summary = "Add members in Group",
		description = "Add members in an existing Group (body: participantIds)",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_U')")
	@PostMapping("/{id}/members")
	fun addMembersToGroupById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid members: GroupMembersWriterDto,
	): Mono<ResponseEntity<AddedGroupMembersReaderDto>>

	@Operation(
		summary = "Remove member from Group",
		description = "Remove member from an existing Group",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_U')")
	@RateLimited(SENSITIVE)
	@DeleteMapping("/{id}/members/{memberId}")
	fun removeMemberFromGroupById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@PathVariable memberId: UUID,
	): Mono<GroupWithoutMemberReaderDto>

	@Operation(
		summary = "Disable Group",
		description = "Disable Group, it will not be visible anymore in the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_U')")
	@RateLimited(SENSITIVE)
	@PostMapping("/{id}/disable")
	fun disableGroupById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<GroupWithoutMemberReaderDto>

	@Operation(
		summary = "Enable Group",
		description = "Enable Group, it will be visible again in the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_U')")
	@RateLimited(SENSITIVE)
	@PostMapping("/{id}/enable")
	fun enableGroupById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<GroupWithoutMemberReaderDto>

	@Operation(
		summary = "Delete Group",
		description = "Delete all Group data.",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_D')")
	@RateLimited(SENSITIVE)
	@DeleteMapping("/{id}")
	fun deleteGroupById(@PathVariable projectId: UUID, @PathVariable id: UUID): Mono<Unit>
}
