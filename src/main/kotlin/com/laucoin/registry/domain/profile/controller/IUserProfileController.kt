package com.laucoin.registry.domain.profile.controller

import com.laucoin.registry.core.model.util.PageModel
import com.laucoin.registry.domain.profile.model.EnrichedProfileModel
import com.laucoin.registry.domain.profile.model.ProfileModel
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import java.security.Principal
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Mono

@Tag(name = "User's profile", description = "User's event profile management APIs")
@RequestMapping("/profiles/user/{userId}")
@PreAuthorize("hasPermission(#userId, 'ROLE_REGISTRY_USER') || hasRole('ROLE_REGISTRY_GLOBAL_USER_MANAGEMENT')")
interface IUserProfileController {
    @Operation(summary = "User's profile page", description = "Get a page of user profiles with pagination and search")
    @GetMapping
    fun getPage(
        @PathVariable userId: UUID,
        @RequestParam pageIndex: Int,
        @RequestParam pageSize: Int,
        @RequestParam(defaultValue = "ASC") order: Direction,
        @RequestParam(defaultValue = "true") onlyAccepted: Boolean,
        @RequestParam(required = false) searched: String?,
        @RequestParam(required = false) startAccess: LocalDateTime?,
        @RequestParam(required = false) endAccess: LocalDateTime?,
    ): Mono<PageModel<EnrichedProfileModel>>

    @Operation(summary = "User", description = "Get a user by id")
    @GetMapping("/{id}")
    fun findById(
        @PathVariable userId: UUID,
        @PathVariable id: UUID,
        principal: Principal,
    ): Mono<EnrichedProfileModel>

    @Operation(summary = "Manage profile acceptance", description = "Accept or reject a profile, cant be rollback")
    @PostMapping("/{id}/acceptance/{accepted}")
    fun manageProfileAcceptance(
        @PathVariable userId: UUID,
        @PathVariable id: UUID,
        @PathVariable accepted: Boolean,
        principal: Principal,
    ): Mono<ProfileModel>

    @Operation(summary = "Delete a profile", description = "Delete a profile, it will remove profile")
    @DeleteMapping("/{id}")
    fun deleteById(
        @PathVariable userId: UUID,
        @PathVariable id: UUID,
        principal: Principal,
    ): Mono<Void>
}
