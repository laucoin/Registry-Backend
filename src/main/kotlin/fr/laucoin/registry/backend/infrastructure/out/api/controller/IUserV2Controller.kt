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
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.UserWriterDto
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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * API v2 Users contract — the ADR 017 reference resource:
 * - list grammar: `page`/`size`/`sort=field,other`/`direction=ASC|DESC`/`q`/typed filters (§5)
 * - state transitions as explicit `POST` actions, no value-in-path (§3)
 * - eligibility sub-collections named for their relationship (§4)
 * - plain field edits as `PATCH /{id}` with a body of changed fields (§3)
 */
@Tag(name = "Users management", description = "API for Users-related operations")
@RequestMapping("/api/v2/users")
interface IUserV2Controller {
	@Operation(
		summary = "Find Users",
		description = "Paginated Users",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_R')")
	@RateLimited(SEARCH, whenParamPresent = ["q"])
	@GetMapping
	fun findUsers(
		@RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) page: Int,
		@RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) size: Int,
		@RequestParam(required = false) sort: List<String>?,
		@RequestParam(required = false, defaultValue = "ASC") direction: String,
		@RequestParam(required = false) q: String?,
		@RequestParam(required = false) visible: Boolean?,
	): Mono<PageModel<UserReaderDto>>

	@Operation(
		summary = "Find User",
		description = "Find User by ID",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_R')")
	@GetMapping("/{id}")
	fun findUserById(@PathVariable id: UUID): Mono<UserReaderDto>

	@Operation(
		summary = "Get assignable Roles",
		description = "Roles the current user is allowed to assign",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_METADATA_R')")
	@RateLimited(SEARCH)
	@GetMapping("/assignable-roles")
	fun getAssignableUserRoles(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
	): Flux<LabelDto>

	@Operation(
		summary = "Update User",
		description = "Update a User's editable fields (currently: role)",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_U')")
	@PatchMapping("/{id}")
	fun updateUser(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
		@RequestBody @Valid user: UserWriterDto,
	): Mono<UserReaderDto>

	@Operation(
		summary = "Block User",
		description = "Prevent a User from logging in",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_U')")
	@RateLimited(SENSITIVE)
	@PostMapping("/{id}/block")
	fun blockUserById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<UserReaderDto>

	@Operation(
		summary = "Unblock User",
		description = "Re-authorize a User to log in",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_U')")
	@RateLimited(SENSITIVE)
	@PostMapping("/{id}/unblock")
	fun unblockUserById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<UserReaderDto>

	@Operation(
		summary = "Delete User",
		description = "Erase a User: the account, its preferences and its project memberships go; the records it created survive with no author named",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_D')")
	@RateLimited(SENSITIVE)
	@DeleteMapping("/{id}")
	fun deleteUserById(@AuthenticationPrincipal currentUser: CurrentUserModel, @PathVariable id: UUID): Mono<Unit>

	/**
	 * Mapped on the literal "/me" rather than the caller's id so the self-service
	 * right needs no id at all — and so it never collides with the administrative
	 * delete above, which the not-current-user guard refuses for one's own row.
	 */
	@Operation(
		summary = "Delete Current User",
		description = "Erase the caller's own account. Same effect as an administrator deleting it; the last-administrator guards still apply",
	)
	@PreAuthorize("isAuthenticated()")
	@RateLimited(SENSITIVE)
	@DeleteMapping("/me")
	fun deleteCurrentUser(@AuthenticationPrincipal currentUser: CurrentUserModel): Mono<Unit>
}
