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
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectProfileReaderDto
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
		description = "Find or get paginated User's Profiles",
	)
	@RateLimited(SEARCH, whenParamPresent = ["q"])
	@GetMapping
	fun findUserProjectProfiles(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@ParameterObject @Valid page: SortedPageQueryDto,
		@RequestParam(required = false) q: String?,
		@RequestParam(required = false) available: Boolean?,
		@RequestParam(required = false) status: ProfileStatusEnum?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) dateTime: ZonedDateTime?,
		@RequestParam(required = false) favorite: Boolean?,
	): Mono<PageReaderDto<ProjectProfileReaderDto>>

	@Operation(
		summary = "Accept Project's invitation",
		description = "Allow User to access the concerned Project",
	)
	@RateLimited(SENSITIVE)
	@PostMapping("/{id}/accept")
	fun acceptUserProjectProfileById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<ProjectProfileReaderDto>

	@Operation(
		summary = "Reject Project's invitation",
		description = "Deny User access to the concerned Project",
	)
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
	@RateLimited(SENSITIVE)
	@PostMapping("/{projectId}/support")
	fun createSupportProjectProfile(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
	): Mono<ProjectProfileReaderDto>

	@Operation(
		summary = "Toggle favorite on User's Profile",
		description = "Star or unstar the Project behind this Profile on the caller's home dashboard",
	)
	@RateLimited(SENSITIVE)
	@PostMapping("/{id}/favorite")
	fun toggleFavoriteUserProjectProfileById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<ProjectProfileReaderDto>

	@Operation(
		summary = "Delete User's Profile",
		description = "Delete User's Profile",
	)
	@RateLimited(SENSITIVE)
	@ResponseStatus(NO_CONTENT)
	@DeleteMapping("/{id}")
	fun deleteUserProfileById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<Unit>
}
