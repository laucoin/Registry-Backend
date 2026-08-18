package fr.laucoin.registry.backend.infrastructure.out.api.controller

import fr.laucoin.registry.backend.domain.annotation.RateLimited
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_D
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_METADATA_R
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_R
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_U
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SEARCH
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SENSITIVE
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.UserReaderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Deprecated(
	"API v1 has no remaining Registry-Frontend consumer and is scheduled for removal; use the /api/v2 contract.",
	level = DeprecationLevel.WARNING,
)
@Tag(name = "Users management (v1, deprecated)", description = "API for Users-related operations — deprecated, scheduled for removal; use /api/v2")
@RequestMapping("/api/v1/users")
interface IUserV1Controller {
	@Operation(
		summary = "Find Users",
		description = "Find or get paginated Users",
		deprecated = true,
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_R')")
	@RateLimited(SEARCH, whenParamPresent = ["textSearched"])
	@GetMapping
	fun findUsers(
		@RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) pageNumber: Int,
		@RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) pageSize: Int,
		@RequestParam(required = false) textSearched: String?,
		@RequestParam(required = false) visibilitySearched: Boolean?,
	): Mono<PageModel<UserReaderDto>>

	@Operation(
		summary = "Find User",
		description = "Find User by ID",
		deprecated = true,
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_R')")
	@GetMapping("/{id}")
	fun findUserById(@PathVariable id: UUID): Mono<UserReaderDto>

	@Operation(
		summary = "Get assignable Roles",
		description = "Get all the roles you are allowed to assign",
		deprecated = true,
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_METADATA_R')")
	@GetMapping("/roles")
	fun getAssignableUserRoles(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
	): Flux<LabelDto>

	@Operation(
		summary = "Update User's role",
		description = "Update a User's role",
		deprecated = true,
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_U')")
	@PatchMapping("/{id}/role")
	fun updateUserRole(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
		@RequestParam(required = false) role: String?,
	): Mono<UserReaderDto>

	@Operation(
		summary = "Block User",
		description = "Prproject a User from logging in",
		deprecated = true,
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_U')")
	@RateLimited(SENSITIVE)
	@PatchMapping("/{id}/block")
	fun blockUserById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<UserReaderDto>

	@Operation(
		summary = "Unblock User",
		description = "Re-authorize a User to log in",
		deprecated = true,
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_U')")
	@RateLimited(SENSITIVE)
	@PatchMapping("/{id}/unblock")
	fun unblockUserById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<UserReaderDto>

	@Operation(
		summary = "Impersonate User",
		description = "Impersonate all User data",
		deprecated = true,
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_D')")
	@PatchMapping("/{id}/impersonate")
	fun impersonateUserById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<UserReaderDto>

	@Operation(
		summary = "Impersonate Current User",
		description = "Impersonate all Current User data",
		deprecated = true,
	)
	@PatchMapping("/impersonate")
	fun impersonateCurrentUser(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
	): Mono<UserReaderDto>

	@Operation(
		summary = "Delete User",
		description = "Delete all User data",
		deprecated = true,
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_D')")
	@RateLimited(SENSITIVE)
	@DeleteMapping("/{id}")
	fun deleteUserById(@AuthenticationPrincipal currentUser: CurrentUserModel, @PathVariable id: UUID): Mono<Unit>
}
