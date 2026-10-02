package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.annotation.RateLimited
import fr.laucoin.registry.backend.domain.constant.ApiConst.API_V2
import fr.laucoin.registry.backend.domain.constant.ApiConst.DEFAULT_DASHBOARD_LIMIT
import fr.laucoin.registry.backend.domain.constant.ApiConst.MAX_DASHBOARD_LIMIT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_EMPTY
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_EXCEEDS_MAX_PAGE_SIZE
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
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.PageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.SortedPageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AddedGroupMembersReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.GroupReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.GroupWithoutMemberReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PageReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.GroupWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
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

@Tag(name = "Participant's groups management", description = "API for Group-related operations")
@RequestMapping("$API_V2/projects/{projectId}/groups")
interface IGroupV2Controller {
	@Operation(
		summary = "Find Groups",
		description = """
			Search and list the Project's Groups (without their members), with pagination and sorting. Combine `q`
			(free-text search), `visible`, `present` (whether the Group's own presence window covers `dateTime`, defaulting to now)
			to narrow the results.
		""",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_R')")
	@RateLimited(SEARCH, whenParamPresent = ["q"])
	@GetMapping
	fun findGroups(
		@PathVariable projectId: UUID,
		@ParameterObject @Valid page: SortedPageQueryDto,
		@RequestParam(name = "q", required = false) query: String?,
		@RequestParam(name = "visible", required = false) isVisible: Boolean?,
		@RequestParam(name = "present", required = false) isPresent: Boolean?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) dateTime: ZonedDateTime?,
	): Mono<PageReaderDto<GroupWithoutMemberReaderDto>>

	@Operation(
		summary = "Find Groups arriving today",
		description = """
			Dashboard widget: Groups whose own presence window opens today and governs at least one Participant's presence
			(i.e. a member with no individual date overriding the Group's). Results are capped at "limit" rows
			(default $DEFAULT_DASHBOARD_LIMIT, max $MAX_DASHBOARD_LIMIT).
		""",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_R')")
	@GetMapping("/arrivals-today")
	fun findGroupsArrivingToday(
		@PathVariable projectId: UUID,
		@RequestParam(defaultValue = DEFAULT_DASHBOARD_LIMIT)
		@Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
			MAX_DASHBOARD_LIMIT,
			message = PAGE_SIZE_EXCEEDS_MAX_PAGE_SIZE
		)
		limit: Int,
	): Flux<GroupWithoutMemberReaderDto>

	@Operation(
		summary = "Find Groups departing today",
		description = """
			Dashboard widget: Groups whose own presence window closes today and governs at least one Participant's presence
			(i.e. a member with no individual date overriding the Group's). Results are capped at "limit" rows
			(default $DEFAULT_DASHBOARD_LIMIT, max $MAX_DASHBOARD_LIMIT).
		""",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_R')")
	@GetMapping("/departures-today")
	fun findGroupsDepartingToday(
		@PathVariable projectId: UUID,
		@RequestParam(defaultValue = DEFAULT_DASHBOARD_LIMIT)
		@Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
			MAX_DASHBOARD_LIMIT,
			message = PAGE_SIZE_EXCEEDS_MAX_PAGE_SIZE
		)
		limit: Int,
	): Flux<GroupWithoutMemberReaderDto>

	@Operation(
		summary = "Find Group Members",
		description = """
			List, paginated, the Participants who are members of this Group. Combine `q` (free-text search), `isMajor`,
			`type` (REGISTERED / GUEST), `visible`, `status` (presence) and `dateTime` to narrow the results.
		""",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_R')")
	@RateLimited(SEARCH, whenParamPresent = ["q"])
	@GetMapping("/{id}/members")
	fun findGroupMembersByGroupId(
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@ParameterObject @Valid page: PageQueryDto,
		@RequestParam(name = "q", required = false) query: String?,
		@RequestParam(required = false) isMajor: Boolean?,
		@RequestParam(required = false) type: ParticipantTypeEnum?,
		@RequestParam(name = "visible", required = false) isVisible: Boolean?,
		@RequestParam(required = false) status: PresenceStatusEnum?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) dateTime: ZonedDateTime?,
	): Mono<PageReaderDto<ParticipantReaderDto>>

	@Operation(
		summary = "Find Group",
		description = "Get a single Group of the Project by its ID, including its members.",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_R')")
	@GetMapping("/{id}")
	fun findGroupById(
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<GroupReaderDto>

	@Operation(
		summary = "Search Participants",
		description = "Search Participants of the Project not yet in this Group, to add as members.",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_METADATA_R')")
	@RateLimited(SEARCH)
	@GetMapping("/search/participants")
	fun searchParticipants(
		@PathVariable projectId: UUID,
		@RequestParam(name = "q", required = false) query: String?,
	): Flux<ParticipantReaderDto>

	@Operation(
		summary = "Create Group",
		description = """
			Create a new Group linked to the Project, with its own availability window and its initial list of members
			(must contain at least one Participant ID).
		""",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_C')")
	@RateLimited(SENSITIVE)
	@PostMapping
	fun createGroup(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@RequestBody @Valid group: GroupWriterDto,
	): Mono<ResponseEntity<GroupWithoutMemberReaderDto>>

	@Operation(
		summary = "Update Group",
		description = "Update the Group's name, availability window and full member list (replaces the current members).",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_U')")
	@RateLimited(SENSITIVE)
	@PatchMapping("/{id}")
	fun updateGroupById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid group: GroupWriterDto,
	): Mono<GroupWithoutMemberReaderDto>

	@Operation(
		summary = "Add members in Group",
		description = "Add one or more existing Participants as members of the Group, without touching its current members.",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_U')")
	@RateLimited(SENSITIVE)
	@PatchMapping("/{id}/members")
	fun addMembersToGroupById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid @NotEmpty(message = GROUP_MEMBERS_EMPTY) memberIds: List<UUID>,
	): Mono<ResponseEntity<AddedGroupMembersReaderDto>>

	@Operation(
		summary = "Remove member from Group",
		description = "Remove a single Participant from the Group's members, without affecting the Participant itself.",
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
		description = "Soft-delete the Group: it is kept (with its members) but hidden from the Project going forward.",
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
		description = "Reverse a disable: the Group becomes visible in the Project again.",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_D')")
	@RateLimited(SENSITIVE)
	@PostMapping("/{id}/enable")
	fun enableGroupById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<GroupWithoutMemberReaderDto>

	@Operation(
		summary = "Delete Group",
		description = """
			Permanently delete the Group and all its data. This cannot be undone; the members themselves are not deleted.
			Prefer disabling the Group if it may be needed again.
		""",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_D')")
	@RateLimited(SENSITIVE)
	@ResponseStatus(NO_CONTENT)
	@DeleteMapping("/{id}")
	fun deleteGroupById(@PathVariable projectId: UUID, @PathVariable id: UUID): Mono<Unit>
}
