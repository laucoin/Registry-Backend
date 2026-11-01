package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_EMPTY
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_TOO_MANY
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_UPPER_THAN_MAX_PAGE
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_DEFAULT
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_MIN
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_SIZE_DEFAULT
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_SIZE_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_SIZE_MIN
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_U
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.GROUP_MEMBER_IDS_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SEARCH_QUERY_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SORT_MAX
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AddedGroupMembersReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.GroupReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.GroupWithoutMemberReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.GroupWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.ZonedDateTime
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
 * Objectif : Contrat REST des endpoints du groupe sous /api/v2.
 * Périmètre : Porte les routes, les règles d'autorisation, les bornes de validation et les métadonnées OpenAPI.
 * Limites : Ne déclare aucun comportement ; l'implémentation du contrôleur délègue aux services.
 */
@Tag(name = "Participant's groups management", description = "API for Group-related operations")
@RequestMapping("/api/v2/projects/{projectId}/groups")
interface IGroupV2Controller {
	@Operation(
		summary = "Find Groups",
		description = "Find or get paginated Groups",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_R')")
	@GetMapping
	fun findGroups(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@RequestParam(defaultValue = PAGE_DEFAULT) @Valid @Min(PAGE_MIN, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) @Max(PAGE_MAX, message = PAGE_NUMBER_IS_UPPER_THAN_MAX_PAGE) page: Long,
		@RequestParam(defaultValue = PAGE_SIZE_DEFAULT) @Valid @Min(PAGE_SIZE_MIN, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(PAGE_SIZE_MAX, message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE) size: Int,
		@Parameter(description = "Text matched against the group name (accent-insensitive substring)")
		@RequestParam(value = "q", required = false) @Valid @Size(max = SEARCH_QUERY_MAX, message = PARAMETER_TOO_LONG) query: String?,
		@Parameter(description = "Visibility filter: true keeps visible elements only, false keeps hidden ones only, omitted keeps both")
		@RequestParam(required = false) isVisible: Boolean?,
		@Parameter(description = "Group availability filter at the current time: true keeps groups currently in their availability window, false keeps the others, omitted keeps both")
		@RequestParam(required = false) isPresent: Boolean?,
		@Parameter(description = "Instant that must fall inside the validity window")
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) at: ZonedDateTime?,
		@Parameter(description = "Membership filter: true keeps the groups having a member linked to the signed-in user, omitted or false keeps every group")
		@RequestParam(required = false) mine: Boolean?,
		@Parameter(description = "On-site members filter: true keeps the groups having a member that arrived and has not left yet, false keeps the others, omitted keeps both")
		@RequestParam(required = false) hasOnSiteMembers: Boolean?,
		@Parameter(description = "Sort field, prefixed with - for descending order. Supported: name, startAvailability, endAvailability")
		@RequestParam(required = false) @Valid @Size(max = SORT_MAX, message = PARAMETER_TOO_LONG) sort: String?,
	): Mono<PageModel<GroupWithoutMemberReaderDto>>

	@Operation(
		summary = "Find Group Members",
		description = "Find or get paginated Group Members by Group ID",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_R')")
	@GetMapping("/{id}/members")
	fun findGroupMembersByGroupId(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestParam(defaultValue = PAGE_DEFAULT) @Valid @Min(PAGE_MIN, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) @Max(PAGE_MAX, message = PAGE_NUMBER_IS_UPPER_THAN_MAX_PAGE) page: Long,
		@RequestParam(defaultValue = PAGE_SIZE_DEFAULT) @Valid @Min(PAGE_SIZE_MIN, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(PAGE_SIZE_MAX, message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE) size: Int,
		@Parameter(description = "Text matched against the member first name and last name")
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
		summary = "Find Group",
		description = "Find Group by ID",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_R')")
	@GetMapping("/{id}")
	fun findGroupById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<GroupReaderDto>

	@Operation(
		summary = "Search Participants",
		description = "Search Participants to add in a Group",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_METADATA_R')")
	@GetMapping("/search/participants")
	fun searchParticipants(
		@PathVariable projectId: UUID,
		@Parameter(description = "Text matched against the participant first name and last name")
		@RequestParam("q") @Valid @Size(max = SEARCH_QUERY_MAX, message = PARAMETER_TOO_LONG) query: String?
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
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<GroupWithoutMemberReaderDto>>

	@Operation(
		summary = "Update Group",
		description = "Update Group",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_U')")
	@PatchMapping("/{id}")
	fun updateGroupById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid group: GroupWriterDto,
		exchange: ServerWebExchange,
	): Mono<GroupWithoutMemberReaderDto>

	@Operation(
		summary = "Add members in Group",
		description = "Add members in an existing Group",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_U')")
	@PostMapping("/{id}/members")
	fun addMembersToGroupById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid @NotEmpty(message = GROUP_MEMBERS_EMPTY) @Size(max = GROUP_MEMBER_IDS_MAX, message = GROUP_MEMBERS_TOO_MANY) memberIds: List<UUID>,
	): Mono<ResponseEntity<AddedGroupMembersReaderDto>>

	@Operation(
		summary = "Remove member from Group",
		description = "Remove member from an existing Group",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_U')")
	@DeleteMapping("/{id}/members/{memberId}")
	@ResponseStatus(NO_CONTENT)
	fun removeMemberFromGroupById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@PathVariable memberId: UUID,
	): Mono<Unit>

	@Operation(
		summary = "Disable Group",
		description = "Disable Group, it will not visible anymore in the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_U')")
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
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_D')")
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
	@DeleteMapping("/{id}")
	@ResponseStatus(NO_CONTENT)
	fun deleteGroupById(@PathVariable projectId: UUID, @PathVariable id: UUID): Mono<Unit>
}
