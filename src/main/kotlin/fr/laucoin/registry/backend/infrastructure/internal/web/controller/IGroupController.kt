package fr.laucoin.registry.backend.infrastructure.internal.web.controller

import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_GROUP_C
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_GROUP_D
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_GROUP_METADATA_R
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_GROUP_R
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_GROUP_U
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.AddedGroupMembersReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.GroupReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.GroupWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Tag(name = "Participant's groups management", description = "API for Group-related operations")
@RequestMapping("/api/events/{eventId}/groups")
interface IGroupController {
    @Operation(
        summary = "Find Groups",
        description = "Find or get paginated Groups",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_GROUP_R')")
    @GetMapping
    fun findGroups(
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "DESC") order: Direction,
        @RequestParam(defaultValue = "true") onlyVisible: Boolean,
        @RequestParam(defaultValue = "false") onlyPresent: Boolean,
        @RequestParam(required = false) searched: String?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DATE_TIME) startDateTime: ZonedDateTime?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DATE_TIME) endDateTime: ZonedDateTime?,
    ): Mono<PageDto<GroupReaderDto>>

    @Operation(
        summary = "Find Group Members",
        description = "Find or get paginated Group Members by Group ID",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_GROUP_R')")
    @GetMapping("/{id}/members")
    fun findGroupMembersByGroupId(
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "DESC") order: Direction,
        @RequestParam(defaultValue = "true") onlyVisible: Boolean,
        @RequestParam(defaultValue = "false") onlyPresent: Boolean,
        @RequestParam(required = false) searched: String?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DATE_TIME) startDateTime: ZonedDateTime?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DATE_TIME) endDateTime: ZonedDateTime?,
    ): Mono<PageDto<ParticipantReaderDto>>

    @Operation(
        summary = "Find Group",
        description = "Find Group by ID",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_GROUP_R')")
    @GetMapping("/{id}")
    fun findGroupById(
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
    ): Mono<GroupReaderDto>

    @Operation(
        summary = "Search Participants",
        description = "Search Participants to add in a Group",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_GROUP_METADATA_R')")
    @GetMapping("/search/participants")
    fun searchParticipants(
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @RequestParam searched: String?
    ): Flux<ParticipantReaderDto>

    @Operation(
        summary = "Create Group",
        description = "Create Group and related Group Content",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_GROUP_C')")
    @PostMapping
    fun createGroup(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @RequestBody @Valid group: GroupWriterDto,
    ): Mono<GroupReaderDto>

    @Operation(
        summary = "Update Group",
        description = "Update Group",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_GROUP_U')")
    @PatchMapping("/{id}")
    fun updateGroupById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
        @RequestBody @Valid group: GroupWriterDto,
    ): Mono<GroupReaderDto>

    @Operation(
        summary = "Add members in Group",
        description = "Add members in an existing Group",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_GROUP_U')")
    @PatchMapping("/{id}/members")
    fun addMembersToGroupById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
        @RequestBody @Valid memberIds: List<UUID>,
    ): Mono<ResponseEntity<AddedGroupMembersReaderDto>>

    @Operation(
        summary = "Remove member from Group",
        description = "Remove member from an existing Group",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_GROUP_U')")
    @DeleteMapping("/{id}/members/{memberId}")
    fun removeMemberFromGroupById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
        @PathVariable memberId: UUID,
    ): Mono<GroupReaderDto>

    @Operation(
        summary = "Disable Group",
        description = "Disable Group, it will not visible anymore in the Event",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_GROUP_U')")
    @PatchMapping("/{id}/disable")
    fun disableGroupById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
    ): Mono<GroupReaderDto>

    @Operation(
        summary = "Enable Group",
        description = "Enable Group, obviously it will be visible again in the Event",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_GROUP_U')")
    @PatchMapping("/{id}/enable")
    fun enableGroupById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
    ): Mono<GroupReaderDto>

    @Operation(
        summary = "Delete Group",
        description = "Delete all Group data.",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_GROUP_D')")
    @DeleteMapping("/{id}")
    fun deleteGroupById(@PathVariable eventId: UUID, @PathVariable id: UUID): Mono<Void>
}
