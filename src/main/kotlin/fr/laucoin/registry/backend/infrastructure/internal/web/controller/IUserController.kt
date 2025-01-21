package fr.laucoin.registry.backend.infrastructure.internal.web.controller

import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_D
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_METADATA_R
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_R
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_U
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Mono

@Tag(name = "Users management", description = "API for Users-related operations")
@RequestMapping("/api/users")
interface IUserController {
    @Operation(
        summary = "Find Users",
        description = "Find or get paginated Users"
    )
    @PreAuthorize("hasAuthority('$REGISTRY_USER_R')")
    @GetMapping
    fun findUsers(
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "ASC") order: Direction,
        @RequestParam(defaultValue = "true") onlyVisible: Boolean,
        @RequestParam(required = false) searched: String?,
    ): Mono<PageDto<UserModel>>

    @Operation(
        summary = "Find User",
        description = "Find User by ID"
    )
    @PreAuthorize("hasAuthority('$REGISTRY_USER_R')")
    @GetMapping("/{id}")
    fun findUserById(@PathVariable id: UUID): Mono<UserModel>

    @Operation(
        summary = "Get assignable Roles",
        description = "Get all the roles you are allowed to assign"
    )
    @PreAuthorize("hasAuthority('$REGISTRY_USER_METADATA_R')")
    @GetMapping("/roles")
    fun getAssignableUserRoles(@AuthenticationPrincipal currentUser: CurrentUserModel): Mono<List<String>>

    @Operation(
        summary = "Update User's role",
        description = "Update a User's role"
    )
    @PreAuthorize("hasAuthority('$REGISTRY_USER_U')")
    @PatchMapping("/{id}/role")
    fun updateUserRole(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @PathVariable id: UUID,
        @RequestParam(required = false) role: String?,
    ): Mono<UserModel>

    @Operation(
        summary = "Block User",
        description = "Prevent a User from logging in"
    )
    @PreAuthorize("hasAuthority('$REGISTRY_USER_U')")
    @PatchMapping("/{id}/block")
    fun blockUserById(@AuthenticationPrincipal currentUser: CurrentUserModel, @PathVariable id: UUID): Mono<UserModel>

    @Operation(
        summary = "Unblock User",
        description = "Re-authorize a User to log in"
    )
    @PreAuthorize("hasAuthority('$REGISTRY_USER_U')")
    @PatchMapping("/{id}/unblock")
    fun unblockUserById(@AuthenticationPrincipal currentUser: CurrentUserModel, @PathVariable id: UUID): Mono<UserModel>

    @Operation(
        summary = "Impersonate User",
        description = "Impersonate all User data"
    )
    @PreAuthorize("hasAuthority('$REGISTRY_USER_U')")
    @PatchMapping("/{id}/impersonate")
    fun impersonateUserById(@AuthenticationPrincipal currentUser: CurrentUserModel, @PathVariable id: UUID): Mono<UserModel>

    @Operation(
        summary = "Delete User",
        description = "Delete all User data"
    )
    @PreAuthorize("hasAuthority('$REGISTRY_USER_D')")
    @DeleteMapping("/{id}")
    fun deleteUserById(@AuthenticationPrincipal currentUser: CurrentUserModel, @PathVariable id: UUID): Mono<Void>
}
