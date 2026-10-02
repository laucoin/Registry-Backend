package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.annotation.RateLimited
import fr.laucoin.registry.backend.domain.constant.ApiConst.API_V2
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_D
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_METADATA_R
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_R
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_U
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SEARCH
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SENSITIVE
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.SortedPageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PageReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.UserDataExportReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.UserReaderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Tag(name = "Users management", description = "API for Users-related operations")
@RequestMapping("$API_V2/users")
interface IUserV2Controller {
	@Operation(
		summary = "Find Users",
		description = """
			Search and list every User of the platform, with pagination and sorting.
			Combine `q` (free-text search) and different filters to narrow the results.
		""",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_R')")
	@RateLimited(SEARCH, whenParamPresent = ["q"])
	@GetMapping
	fun findUsers(
		@ParameterObject @Valid page: SortedPageQueryDto,
		@RequestParam(name = "q", required = false) query: String?,
		@RequestParam(required = false) visible: Boolean?,
	): Mono<PageReaderDto<UserReaderDto>>

	@Operation(
		summary = "Find User",
		description = "Get a single User of the platform by its ID.",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_R')")
	@GetMapping("/{id}")
	fun findUserById(@PathVariable id: UUID): Mono<UserReaderDto>

	@Operation(
		summary = "Get assignable Roles",
		description = "List the platform-level roles the caller is allowed to assign to another User.",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_METADATA_R')")
	@GetMapping("/metadata/roles")
	fun getAssignableUserRoles(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
	): Flux<LabelDto>

	@Operation(
		summary = "Update User's role",
		description = "Change a User's platform-level role. Pass `role` blank/omitted to remove their current role.",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_U')")
	@RateLimited(SENSITIVE)
	@PatchMapping("/{id}/role")
	fun updateUserRole(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
		@RequestParam(required = false) role: String?,
	): Mono<UserReaderDto>

	@Operation(
		summary = "Block User",
		description = "Prevent the User from logging in, without deleting their account or data.",
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
		description = "Reverse a block: the User can log in again.",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_U')")
	@RateLimited(SENSITIVE)
	@PostMapping("/{id}/unblock")
	fun unblockUserById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<UserReaderDto>

	@Operation(
		summary = "Impersonate User",
		description = """
			Start a support session as this User: subsequent authenticated calls act on their behalf. Intended for
			platform administrators investigating an issue on a User's account.
		""",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_D')")
	@RateLimited(SENSITIVE)
	@PostMapping("/{id}/impersonate")
	fun impersonateUserById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<UserReaderDto>

	@Operation(
		summary = "Impersonate Current User",
		description = """
			Re-authenticate as the caller's own account, ending any ongoing impersonation session started via
			"Impersonate User" and returning to the caller's own identity.
		""",
	)
	@RateLimited(SENSITIVE)
	@PostMapping("/impersonate")
	fun impersonateCurrentUser(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
	): Mono<UserReaderDto>

	@Operation(
		summary = "Export Current User data",
		description = "Export all personal data held about the caller's own account (GDPR access/portability request): account, preferences, project memberships, content authored across every project, and any linked Participant's data",
	)
	@RateLimited(SENSITIVE)
	@PostMapping("/me/data-export")
	fun exportCurrentUserData(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
	): Mono<UserDataExportReaderDto>

	@Operation(
		summary = "Delete User",
		description = """
			Permanently delete the User account and all its data, including their Profiles across every Project.
			This cannot be undone; prefer blocking the User if it may need to be reversed.
		""",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_D')")
	@RateLimited(SENSITIVE)
	@ResponseStatus(NO_CONTENT)
	@DeleteMapping("/{id}")
	fun deleteUserById(@AuthenticationPrincipal currentUser: CurrentUserModel, @PathVariable id: UUID): Mono<Unit>
}
