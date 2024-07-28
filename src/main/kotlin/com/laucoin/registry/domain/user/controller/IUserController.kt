package com.laucoin.registry.domain.user.controller

import com.laucoin.registry.core.model.user.EnrichedUserModel
import com.laucoin.registry.core.model.user.UserModel
import com.laucoin.registry.core.model.util.PageModel
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.security.Principal
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Mono

@Tag(name = "User", description = "User management APIs")
@RequestMapping("/users")
interface IUserController {
    @Operation(summary = "Users page", description = "Get a page of users with pagination and search")
    @GetMapping
    @PreAuthorize("hasRole('ROLE_REGISTRY_GLOBAL_USER_MANAGEMENT')")
    fun getPage(
        @RequestParam pageIndex: Int,
        @RequestParam pageSize: Int,
        @RequestParam(defaultValue = "ASC") order: Direction,
        @RequestParam(defaultValue = "true") onlyNonBlocked: Boolean,
        @RequestParam(required = false) searched: String?,
        principal: Principal,
    ): Mono<PageModel<EnrichedUserModel>>

    @Operation(summary = "User details", description = "Get your own user details")
    @GetMapping("/me")
    fun getMe(principal: Principal): EnrichedUserModel

    @Operation(summary = "User", description = "Get a user by id")
    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(#id, 'ROLE_REGISTRY_USER') || hasRole('ROLE_REGISTRY_GLOBAL_USER_MANAGEMENT')")
    fun findById(
        @PathVariable id: UUID,
        principal: Principal,
    ): Mono<EnrichedUserModel>

    @Operation(summary = "Users email", description = "Find users email with a text search")
    @GetMapping("/emails")
    fun findAccountEmailBySearch(
        @Parameter(description = "Email search text (min 3 characters)")
        @RequestParam @NotNull @Size(min = 3) searched: String,
        principal: Principal,
    ): Mono<List<String?>>

    @Operation(summary = "User roles", description = "Get all available roles for a user")
    @GetMapping("/roles")
    @PreAuthorize("hasRole('ROLE_REGISTRY_GLOBAL_USER_MANAGEMENT')")
    fun getRoles(principal: Principal): Mono<List<String?>>

    @Operation(summary = "Update user's role", description = "Update user's role to a new one, it obviously change his permissions")
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ROLE_REGISTRY_GLOBAL_USER_MANAGEMENT')")
    fun updateRole(
        @PathVariable id: UUID,
        @RequestParam(required = false) role: String?,
        principal: Principal,
    ): Mono<UserModel>

    @Operation(
        summary = "Update user's default profile",
        description = "Update user's default profile, used to set default profile value in user"
    )
    @PatchMapping("/{id}/profiles/{profileId}/default")
    @PreAuthorize("hasPermission(#id, 'ROLE_REGISTRY_USER')")
    fun updateDefaultProfile(
        @PathVariable id: UUID,
        @PathVariable profileId: UUID,
        principal: Principal,
    ): Mono<UserModel>

    @Operation(summary = "Block a user", description = "Block a user, he will not be able to login anymore")
    @PatchMapping("/{id}/block")
    @PreAuthorize("hasRole('ROLE_REGISTRY_GLOBAL_USER_MANAGEMENT')")
    fun blockById(
        @PathVariable id: UUID,
        principal: Principal,
    ): Mono<UserModel>

    @Operation(summary = "Unblock a user", description = "Unblock a user, he will be able to login again")
    @PatchMapping("/{id}/unblock")
    @PreAuthorize("hasRole('ROLE_REGISTRY_GLOBAL_USER_MANAGEMENT')")
    fun unblockById(
        @PathVariable id: UUID,
        principal: Principal,
    ): Mono<UserModel>

    @Operation(summary = "Delete a user", description = "Delete a user, it will remove all his data")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(#id, 'ROLE_REGISTRY_USER') || hasRole('ROLE_REGISTRY_GLOBAL_USER_MANAGEMENT')")
    fun deleteById(
        @PathVariable id: UUID,
        principal: Principal,
    ): Mono<Void>
}
