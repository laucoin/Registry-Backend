package fr.laucoin.registry.backend.infrastructure.out.api.controller

import fr.laucoin.registry.backend.domain.annotation.RateLimited
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_PROFILE_C
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SEARCH
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SENSITIVE
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectProfileReaderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.UUID

/**
 * API v2 "My profiles" contract (ADR 017): the caller's own project profiles.
 * The v1 `accept/{accepted}` boolean-in-path becomes the two explicit
 * transitions `POST /{id}/accept` and `POST /{id}/reject` (§3).
 */
@Tag(name = "User's Profiles management", description = "API for User's Profiles-related operations")
@RequestMapping("/api/v2/users/profiles")
interface IUserProjectProfileV2Controller {
	@Operation(
		summary = "Find User's Profiles",
		description = "Paginated caller's Profiles",
	)
	@PreAuthorize("isAuthenticated()")
	@RateLimited(SEARCH, whenParamPresent = ["q"])
	@GetMapping
	fun findUserProjectProfiles(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) page: Int,
		@RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) size: Int,
		@RequestParam(required = false) q: String?,
		@RequestParam(required = false) available: Boolean?,
		@RequestParam(required = false) status: ProfileStatusEnum?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) dateTime: ZonedDateTime?,
		@RequestParam(required = false) favorite: Boolean?,
	): Mono<PageModel<ProjectProfileReaderDto>>

	@Operation(
		summary = "Find invitations the caller sent",
		description = "Profiles the caller created for others: still pending, or answered within the recency window (home dashboard)",
	)
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/sent")
	fun findSentInvitations(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) page: Int,
		@RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) size: Int,
	): Mono<PageModel<ProjectProfileReaderDto>>

	@Operation(
		summary = "Accept Project's invitation",
		description = "Accept the invitation, allowing the User to access the concerned Project",
	)
	@PreAuthorize("isAuthenticated()")
	@RateLimited(SENSITIVE)
	@PostMapping("/{id}/accept")
	fun acceptUserProjectProfileById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<ProjectProfileReaderDto>

	@Operation(
		summary = "Reject Project's invitation",
		description = "Reject the invitation to the concerned Project",
	)
	@PreAuthorize("isAuthenticated()")
	@RateLimited(SENSITIVE)
	@PostMapping("/{id}/reject")
	fun rejectUserProjectProfileById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<ProjectProfileReaderDto>

	@Operation(
		summary = "Create support Project's Profile",
		description = "Support profile is a temporary Profile for an User to access an Project to help the administration",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_PROFILE_C')")
	@PostMapping("/{projectId}/support")
	fun createSupportProjectProfile(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
	): Mono<ProjectProfileReaderDto>

	@Operation(
		summary = "Toggle favorite on the caller's Profile",
		description = "Star/unstar the caller's own membership to pin the Project on their home dashboard",
	)
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/{id}/favorite")
	fun toggleFavoriteUserProjectProfileById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<ProjectProfileReaderDto>

	@Operation(
		summary = "Delete User's Profile",
		description = "Leave a Project by deleting the caller's Profile",
	)
	@PreAuthorize("isAuthenticated()")
	@RateLimited(SENSITIVE)
	@DeleteMapping("/{id}")
	fun deleteUserProfileById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID
	): Mono<Unit>
}
