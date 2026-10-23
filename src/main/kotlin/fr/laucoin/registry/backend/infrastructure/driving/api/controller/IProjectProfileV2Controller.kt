package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_U
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CreatedProjectProfilesReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ProjectProfileWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ProjectProfilesWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.ResponseEntity
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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Tag(name = "Project's Profiles management", description = "API for Project's Profiles-related operations")
@RequestMapping("/api/v2/projects/{projectId}/profiles")
interface IProjectProfileV2Controller {
	@Operation(
		summary = "Find Project's Profiles",
		description = "Find or get paginated Project's Profiles",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PROFILE_R')")
	@GetMapping
	fun findProjectProfiles(
		@PathVariable projectId: UUID,
		@RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) page: Int,
		@RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) size: Int,
		@Parameter(description = "Text matched against the first name, last name and email of the profile's user")
		@RequestParam(value = "q", required = false) @Valid @Size(max = 100, message = PARAMETER_TOO_LONG) query: String?,
		@Parameter(description = "Availability filter at the current time: true keeps available elements only, false keeps unavailable ones only, omitted keeps both")
		@RequestParam(required = false) isAvailable: Boolean?,
		@RequestParam(required = false) status: ProfileStatusEnum?,
		@Parameter(description = "Instant that must fall inside the validity window")
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) at: ZonedDateTime?,
		@Parameter(description = "Exact role of the profile, as listed by the roles endpoint")
		@RequestParam(required = false) @Valid @Size(max = 50, message = PARAMETER_TOO_LONG) role: String?,
		@Parameter(description = "Sort field, prefixed with - for descending order. Supported: firstName, lastName, email, role, status, startAccess, endAccess")
		@RequestParam(required = false) @Valid @Size(max = 32, message = PARAMETER_TOO_LONG) sort: String?,
	): Mono<PageModel<ProjectProfileReaderDto>>

	@Operation(
		summary = "Find Project's Profile",
		description = "Find Project's Profile by ID",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PROFILE_R')")
	@GetMapping("/{id}")
	fun findProjectProfileById(
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<ProjectProfileReaderDto>

	@Operation(
		summary = "Search Users",
		description = "Search Users to invite to an Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PROFILE_METADATA_R')")
	@GetMapping("/search/users")
	fun searchUsers(
		@PathVariable projectId: UUID,
		@Parameter(description = "Text matched against the user first name, last name and email")
		@RequestParam("q") @Valid @Size(max = 100, message = PARAMETER_TOO_LONG) query: String?,
	): Flux<PartialUserReaderDto>

	@Operation(
		summary = "Get assignable Roles",
		description = "Get all the roles you are allowed to assign",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PROFILE_METADATA_R')")
	@GetMapping("/roles")
	fun getAssignableProjectProfileRoles(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
	): Flux<LabelDto>

	@Operation(
		summary = "Create Project's Profiles",
		description = "Create Project's Profiles (multiple Users)",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PROFILE_C')")
	@PostMapping
	fun createProjectProfiles(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@RequestBody @Valid profiles: ProjectProfilesWriterDto,
	): Mono<ResponseEntity<CreatedProjectProfilesReaderDto>>

	@Operation(
		summary = "Update Project's Profile",
		description = "Update Project's Profile",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PROFILE_U')")
	@PatchMapping("/{id}")
	fun updateProjectProfile(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid profile: ProjectProfileWriterDto,
		exchange: ServerWebExchange,
	): Mono<ProjectProfileReaderDto>

	@Operation(
		summary = "Block Project's Profile",
		description = "Prevent a User from using it",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PROFILE_U')")
	@PostMapping("/{id}/block")
	fun blockProjectProfileById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<ProjectProfileReaderDto>

	@Operation(
		summary = "Unblock Project's Profile",
		description = "Re-authorize a User to use it",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PROFILE_U')")
	@PostMapping("/{id}/unblock")
	fun unblockProjectProfileById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<ProjectProfileReaderDto>

	@Operation(
		summary = "Delete Project's Profile",
		description = "Delete Project's Profile",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PROFILE_D')")
	@DeleteMapping("/{id}")
	@ResponseStatus(NO_CONTENT)
	fun deleteProjectProfileById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID
	): Mono<Unit>
}
