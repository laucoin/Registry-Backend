package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.annotation.RateLimited
import fr.laucoin.registry.backend.domain.constant.ApiConst.API_V2
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_PROFILE_C
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SEARCH
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SENSITIVE
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.SortedPageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PageReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.UserProjectProfileReaderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.UUID

@Tag(name = "User's Profiles management", description = "API for User's Profiles-related operations")
@RequestMapping("$API_V2/users/profiles")
interface IUserProjectProfileV2Controller {
	@Operation(
		summary = "Find User's Profiles",
		description = """
			Search and list the caller's own Project Profiles (one per Project they belong to), with pagination and
			sorting. Combine `q` (free-text search), `available`, `upcoming`, `status` (INVITED / ACCEPTED / REJECTED / BLOCKED),
			`dateTime` and `favorite` to narrow the results. Set `includeCounts` to decorate each row with its Project's
			participant/vehicle/group/activity/profile/ongoing-alert counts (adds extra queries per row — leave off unless the
			widget needs them).
		""",
	)
	@RateLimited(SEARCH, whenParamPresent = ["q"])
	@GetMapping
	fun findUserProjectProfiles(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@ParameterObject @Valid page: SortedPageQueryDto,
		@RequestParam(name = "q", required = false) query: String?,
		@RequestParam(required = false) available: Boolean?,
		@RequestParam(required = false) upcoming: Boolean?,
		@RequestParam(required = false) status: ProfileStatusEnum?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) dateTime: ZonedDateTime?,
		@RequestParam(required = false) favorite: Boolean?,
		@RequestParam(required = false) includeCounts: Boolean?,
	): Mono<PageReaderDto<UserProjectProfileReaderDto>>

	@Operation(
		summary = "Accept Project's invitation",
		description = "Accept a pending (INVITED) Profile, moving it to ACCEPTED and granting the caller access to the corresponding Project.",
	)
	@RateLimited(SENSITIVE)
	@PostMapping("/{id}/accept")
	fun acceptUserProjectProfileById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<UserProjectProfileReaderDto>

	@Operation(
		summary = "Reject Project's invitation",
		description = "Decline a pending (INVITED) Profile, moving it to REJECTED and denying the caller access to the corresponding Project.",
	)
	@RateLimited(SENSITIVE)
	@PostMapping("/{id}/reject")
	fun rejectUserProjectProfileById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<UserProjectProfileReaderDto>

	@Operation(
		summary = "Create support Project's Profile",
		description = """
			Create a temporary, self-granted "support" Profile on a Project so the caller can access it to assist its
			administrators, without going through an invitation.
		""",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_PROFILE_C')")
	@RateLimited(SENSITIVE)
	@PostMapping("/{projectId}/support")
	fun createSupportProjectProfile(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
	): Mono<UserProjectProfileReaderDto>

	@Operation(
		summary = "Toggle favorite on User's Profile",
		description = """
			Star or unstar the Project behind this Profile on the caller's home dashboard. Only meaningful for an
			ACCEPTED Profile.
		""",
	)
	@RateLimited(SENSITIVE)
	@PostMapping("/{id}/favorite")
	fun toggleFavoriteUserProjectProfileById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<UserProjectProfileReaderDto>

	@Operation(
		summary = "Delete User's Profile",
		description = """
			Permanently delete the caller's own Profile on a Project (leaving it), removing their access to it.
			This cannot be undone.
		""",
	)
	@RateLimited(SENSITIVE)
	@ResponseStatus(NO_CONTENT)
	@DeleteMapping("/{id}")
	fun deleteUserProfileById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<Unit>
}
