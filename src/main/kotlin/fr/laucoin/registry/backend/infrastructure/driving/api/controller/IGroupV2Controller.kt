package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.annotation.RateLimited
import fr.laucoin.registry.backend.domain.constant.ApiConst.API_V2
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_EMPTY
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
		description = "Find or get paginated Groups",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_R')")
	@RateLimited(SEARCH, whenParamPresent = ["q"])
	@GetMapping
	fun findGroups(
		@PathVariable projectId: UUID,
		@ParameterObject @Valid page: SortedPageQueryDto,
		@RequestParam(required = false) q: String?,
		@RequestParam(required = false) visible: Boolean?,
		@RequestParam(required = false) present: Boolean?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) dateTime: ZonedDateTime?,
	): Mono<PageReaderDto<GroupWithoutMemberReaderDto>>

	@Operation(
		summary = "Find Group Members",
		description = "Find or get paginated Group Members by Group ID",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_R')")
	@GetMapping("/{id}/members")
	fun findGroupMembersByGroupId(
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@ParameterObject @Valid page: PageQueryDto,
		@RequestParam(required = false) q: String?,
		@RequestParam(required = false) isMajor: Boolean?,
		@RequestParam(required = false) type: ParticipantTypeEnum?,
		@RequestParam(required = false) visible: Boolean?,
		@RequestParam(required = false) status: PresenceStatusEnum?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) dateTime: ZonedDateTime?,
	): Mono<PageReaderDto<ParticipantReaderDto>>

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
		summary = "Search Participants",
		description = "Search Participants to add in a Group",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_METADATA_R')")
	@RateLimited(SEARCH)
	@GetMapping("/search/participants")
	fun searchParticipants(
		@PathVariable projectId: UUID,
		@RequestParam q: String?,
	): Flux<ParticipantReaderDto>

	@Operation(
		summary = "Create Group",
		description = "Create Group and related Group Content",
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
		description = "Update Group",
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
		description = "Add members in an existing Group",
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
		description = "Disable Group, it will not visible anymore in the Project",
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
		description = "Enable Group, obviously it will be visible again in the Project",
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
	@ResponseStatus(NO_CONTENT)
	@DeleteMapping("/{id}")
	fun deleteGroupById(@PathVariable projectId: UUID, @PathVariable id: UUID): Mono<Unit>
}
