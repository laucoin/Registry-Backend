package com.laucoin.registry.domain.profile.controller

import com.laucoin.registry.core.model.util.PageModel
import com.laucoin.registry.domain.profile.model.EnrichedProfileModel
import com.laucoin.registry.domain.profile.model.ProfileModel
import com.laucoin.registry.domain.profile.model.ProfilesCreationModel
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.security.Principal
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Mono

@Tag(name = "Event's profile", description = "User's event profile management APIs")
@RequestMapping("/profiles/event/{eventId}")
interface IEventProfileController {
    @Operation(summary = "Event's profile page", description = "Get a page of event profiles with pagination and search")
    @PreAuthorize("hasPermission(#eventId, 'ROLE_REGISTRY_EVENT_RO')")
    @GetMapping
    fun getPage(
        @PathVariable eventId: UUID,
        @RequestParam pageIndex: Int,
        @RequestParam pageSize: Int,
        @RequestParam(defaultValue = "ASC") order: Direction,
        @RequestParam(defaultValue = "true") onlyNonBlocked: Boolean,
        @RequestParam(defaultValue = "true") onlyAccepted: Boolean,
        @RequestParam(required = false) searched: String?,
        @RequestParam(required = false) startAccess: LocalDateTime?,
        @RequestParam(required = false) endAccess: LocalDateTime?,
    ): Mono<PageModel<EnrichedProfileModel>>

    @Operation(summary = "Profile", description = "Get a profile by ID")
    @PreAuthorize("hasPermission(#eventId, 'ROLE_REGISTRY_EVENT_RO')")
    @GetMapping("/{id}")
    fun findById(
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
        principal: Principal,
    ): Mono<EnrichedProfileModel>

    @Operation(summary = "Profile roles", description = "Get all assignable roles for a profile")
    @PreAuthorize("hasPermission(#eventId, 'ROLE_REGISTRY_EVENT_RO')")
    @GetMapping("/roles")
    fun getRoles(
        @PathVariable eventId: UUID,
        @RequestParam(required = false) id: UUID?,
        principal: Principal
    ): Mono<List<String?>>

    @Operation(summary = "Create support profile", description = "Create support profile for an user to access an event to help")
    @PreAuthorize("hasRole('ROLE_REGISTRY_GLOBAL_SUPPORT_PROFILE')")
    @PostMapping("/support")
    fun createSupportProfile(
        @PathVariable eventId: UUID,
        @RequestParam role: String,
        principal: Principal
    ): Mono<ProfileModel>

    @Operation(summary = "Create profile", description = "Create profile for an user to access an event")
    @PreAuthorize("hasPermission(#eventId, 'ROLE_REGISTRY_EVENT_RW')")
    @PostMapping
    fun create(
        @PathVariable eventId: UUID,
        @RequestBody @Valid profiles: ProfilesCreationModel,
        principal: Principal
    ): Mono<ResponseEntity<List<ProfileModel>>>

    @Operation(summary = "Update profile", description = "Update profile for an user to access an event")
    @PreAuthorize("hasPermission(#eventId, 'ROLE_REGISTRY_EVENT_RW')")
    @PutMapping("/{id}")
    fun updateById(
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
        @RequestBody @Valid profile: ProfileModel,
        principal: Principal
    ): Mono<ProfileModel>

    @Operation(
        summary = "Block a profile",
        description = "Block a profile, the related user will not be able to access the event anymore"
    )
    @PreAuthorize("hasPermission(#eventId, 'ROLE_REGISTRY_EVENT_RW')")
    @PatchMapping("/{id}/block")
    fun blockById(
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
        principal: Principal,
    ): Mono<ProfileModel>

    @Operation(
        summary = "Unblock a profile",
        description = "Unblock a profile, the related user will be able to access the event anymore"
    )
    @PreAuthorize("hasPermission(#eventId, 'ROLE_REGISTRY_EVENT_RWD')")
    @PatchMapping("/{id}/unblock")
    fun unblockById(
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
        principal: Principal,
    ): Mono<ProfileModel>

    @Operation(summary = "Delete a profile", description = "Delete a profile, it will remove profile")
    @PreAuthorize("hasPermission(#eventId, 'ROLE_REGISTRY_EVENT_RWD')")
    @DeleteMapping("/{id}")
    fun deleteById(
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
        principal: Principal,
    ): Mono<Void>
}
