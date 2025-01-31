package fr.laucoin.registry.backend.infrastructure.internal.web.controller

import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_D
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_METADATA_R
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_R
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_U
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.UserReaderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.Locale
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Tag(name = "Users management", description = "API for Users-related operations")
@RequestMapping("/api/users")
interface IUserController {
    @Operation(
        summary = "Find Users",
        description = "Find or get paginated Users",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasAuthority('$REGISTRY_USER_R')")
    @GetMapping
    fun findUsers(
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "ASC") order: Direction,
        @RequestParam(defaultValue = "true") onlyVisible: Boolean,
        @RequestParam(required = false) searched: String?,
    ): Mono<PageDto<UserReaderDto>>

    @Operation(
        summary = "Find User",
        description = "Find User by ID",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasAuthority('$REGISTRY_USER_R')")
    @GetMapping("/{id}")
    fun findUserById(@RequestHeader(ACCEPT_LANGUAGE) locale: Locale, @PathVariable id: UUID): Mono<UserReaderDto>

    @Operation(
        summary = "Get assignable Roles",
        description = "Get all the roles you are allowed to assign",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasAuthority('$REGISTRY_USER_METADATA_R')")
    @GetMapping("/roles")
    fun getAssignableUserRoles(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
    ): Flux<LabelDto>

    @Operation(
        summary = "Update User's role",
        description = "Update a User's role",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasAuthority('$REGISTRY_USER_U')")
    @PatchMapping("/{id}/role")
    fun updateUserRole(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable id: UUID,
        @RequestParam(required = false) role: String?,
    ): Mono<UserReaderDto>

    @Operation(
        summary = "Block User",
        description = "Prevent a User from logging in",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasAuthority('$REGISTRY_USER_U')")
    @PatchMapping("/{id}/block")
    fun blockUserById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable id: UUID,
    ): Mono<UserReaderDto>

    @Operation(
        summary = "Unblock User",
        description = "Re-authorize a User to log in",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasAuthority('$REGISTRY_USER_U')")
    @PatchMapping("/{id}/unblock")
    fun unblockUserById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable id: UUID,
    ): Mono<UserReaderDto>

    @Operation(
        summary = "Impersonate User",
        description = "Impersonate all User data",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasAuthority('$REGISTRY_USER_D')")
    @PatchMapping("/{id}/impersonate")
    fun impersonateUserById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable id: UUID,
    ): Mono<UserReaderDto>

    @Operation(
        summary = "Impersonate Current User",
        description = "Impersonate all Current User data",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PatchMapping("/impersonate")
    fun impersonateCurrentUser(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
    ): Mono<UserReaderDto>

    @Operation(
        summary = "Delete User",
        description = "Delete all User data",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasAuthority('$REGISTRY_USER_D')")
    @DeleteMapping("/{id}")
    fun deleteUserById(@AuthenticationPrincipal currentUser: CurrentUserModel, @PathVariable id: UUID): Mono<Void>
}
