package fr.laucoin.registry.backend.infrastructure.internal.web.controller

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.CreatedEventProfilesDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.EventProfileDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.EventProfilesDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Mono

@Tag(name = "Event's Profiles management", description = "API for Event's Profiles-related operations")
@RequestMapping("/api/events/{eventId}/profiles")
interface IEventProfileController {
    @Operation(
        summary = "Find Event's Profiles",
        description = "Find or get paginated Event's Profiles"
    )
    @PreAuthorize("hasPermission(#eventId, 'REGISTRY_EVENT_PROFILE_R')")
    @GetMapping
    fun findEventProfiles(
        @PathVariable eventId: UUID,
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "ASC") order: Direction,
        @RequestParam(defaultValue = "true") onlyVisible: Boolean,
        @RequestParam(required = false) status: ProfileStatusEnum?,
        @RequestParam(required = false) searched: String?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DATE_TIME) startAccess: ZonedDateTime?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DATE_TIME) endAccess: ZonedDateTime?,
    ): Mono<PageModel<EventProfileModel>>

    @Operation(
        summary = "Find Event's Profile",
        description = "Find Event's Profile by ID"
    )
    @PreAuthorize("hasPermission(#eventId, 'REGISTRY_EVENT_PROFILE_R')")
    @GetMapping("/{id}")
    fun findEventProfileById(@PathVariable eventId: UUID, @PathVariable id: UUID): Mono<EventProfileModel>

    @Operation(
        summary = "Get assignable Roles",
        description = "Get all the roles you are allowed to assign"
    )
    @PreAuthorize("hasPermission(#eventId, 'REGISTRY_EVENT_PROFILE_METADATA_R')")
    @GetMapping("/roles")
    fun getAssignableEventProfileRoles(@PathVariable eventId: UUID): Mono<List<String>>

    @Operation(
        summary = "Create Event's Profiles",
        description = "Create Event's Profiles (multiple Users)"
    )
    @PreAuthorize("hasPermission(#eventId, 'REGISTRY_EVENT_PROFILE_C')")
    @PostMapping
    fun createEventProfiles(
        @PathVariable eventId: UUID,
        @RequestBody @Valid profiles: EventProfilesDto,
    ): Mono<ResponseEntity<CreatedEventProfilesDto>>

    @Operation(
        summary = "Create support Event's Profile",
        description = "Support profile is a temporary Profile for an User to access an Event to help the administration"
    )
    @PreAuthorize("hasAuthority('REGISTRY_PROFILE_C')")
    @PostMapping("/support")
    fun createSupportEventProfile(@PathVariable eventId: UUID): Mono<EventProfileModel>

    @Operation(
        summary = "Update Event's Profile",
        description = "Update Event's Profile"
    )
    @PreAuthorize("hasPermission(#eventId, 'REGISTRY_EVENT_PROFILE_U')")
    @PatchMapping("/{id}")
    fun updateEventProfile(
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
        @RequestBody @Valid profile: EventProfileDto,
    ): Mono<EventProfileModel>

    @Operation(
        summary = "Block Event's Profile",
        description = "Prevent a User from using it"
    )
    @PreAuthorize("hasPermission(#eventId, 'REGISTRY_EVENT_PROFILE_U')")
    @PatchMapping("/{id}/block")
    fun blockEventProfileById(@PathVariable eventId: UUID, @PathVariable id: UUID): Mono<EventProfileModel>

    @Operation(
        summary = "Unblock Event's Profile",
        description = "Re-authorize a User to use it"
    )
    @PreAuthorize("hasPermission(#eventId, 'REGISTRY_EVENT_PROFILE_U')")
    @PatchMapping("/{id}/unblock")
    fun unblockEventProfileById(@PathVariable eventId: UUID, @PathVariable id: UUID): Mono<EventProfileModel>

    @Operation(
        summary = "Delete Event's Profile",
        description = "Delete Event's Profile"
    )
    @PreAuthorize("hasPermission(#eventId, 'REGISTRY_EVENT_PROFILE_D')")
    @DeleteMapping("/{id}")
    fun deleteEventProfileById(@PathVariable eventId: UUID, @PathVariable id: UUID): Mono<Void>
}
