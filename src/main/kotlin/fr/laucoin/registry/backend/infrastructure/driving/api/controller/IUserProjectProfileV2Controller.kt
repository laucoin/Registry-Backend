package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_FORMAT_INVALID
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_PROFILE_C
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PROJECT_PROFILE_ROLE_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.ROLE_PATTERN
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SEARCH_QUERY_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SORT_MAX
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectWithProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.UserInvitationReaderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.ZonedDateTime
import java.util.UUID
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

/**
 * Objectif : Contrat REST des endpoints du profil de projet de l'utilisateur sous /api/v2.
 * Périmètre : Porte les routes, les règles d'autorisation, les bornes de validation et les métadonnées OpenAPI.
 * Limites : Ne déclare aucun comportement ; l'implémentation du contrôleur délègue aux services.
 */
@Tag(name = "User's Profiles management", description = "API for User's Profiles-related operations")
@RequestMapping("/api/v2/users/profiles")
interface IUserProjectProfileV2Controller {
	@Operation(
		summary = "Find User's Profiles",
		description = "Find or get paginated User's Profiles",
	)
	@PreAuthorize("isAuthenticated()")
	@GetMapping
	fun findUserProjectProfiles(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) page: Int,
		@RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) size: Int,
		@Parameter(description = "Text matched against the project name (accent-insensitive substring)")
		@RequestParam(value = "q", required = false) @Valid @Size(max = SEARCH_QUERY_MAX, message = PARAMETER_TOO_LONG) query: String?,
		@Parameter(description = "Availability filter at the current time: true keeps available elements only, false keeps unavailable ones only, omitted keeps both")
		@RequestParam(required = false) isAvailable: Boolean?,
		@RequestParam(required = false) status: ProfileStatusEnum?,
		@Parameter(description = "Instant that must fall inside the validity window")
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) at: ZonedDateTime?,
		@Parameter(description = "Exact role of the profile, as listed by the roles endpoint")
		@RequestParam(required = false)
		@Valid
		@Size(max = PROJECT_PROFILE_ROLE_MAX, message = PARAMETER_TOO_LONG)
		@Pattern(regexp = ROLE_PATTERN, message = PARAMETER_FORMAT_INVALID) role: String?,
		@Parameter(description = "Sort field, prefixed with - for descending order. Supported: projectName, role, status, startAccess, endAccess")
		@RequestParam(required = false) @Valid @Size(max = SORT_MAX, message = PARAMETER_TOO_LONG) sort: String?,
	): Mono<PageModel<UserInvitationReaderDto>>

	@Operation(
		summary = "Find User's Profile",
		description = "Find User's Profile (invitation) by ID",
	)
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/{id}")
	fun findUserProjectProfileById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<UserInvitationReaderDto>

	@Operation(
		summary = "Accept or Reject Project's invitation",
		description = "Allow User to access the concerned Project if accepted",
	)
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/{id}/accept/{accepted}")
	fun manageUserProjectProfileAcceptance(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
		@PathVariable accepted: Boolean,
	): Mono<UserInvitationReaderDto>

	@Operation(
		summary = "Create support Project's Profile",
		description = "Support profile is a temporary Profile for an User to access an Project to help the administration",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_PROFILE_C')")
	@PostMapping("/{projectId}/support")
	fun createSupportProjectProfile(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
	): Mono<ProjectWithProfileReaderDto>

	@Operation(
		summary = "Delete User's Profile",
		description = "Delete User's Profile",
	)
	@PreAuthorize("isAuthenticated()")
	@DeleteMapping("/{id}")
	@ResponseStatus(NO_CONTENT)
	fun deleteUserProfileById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID
	): Mono<Unit>
}
