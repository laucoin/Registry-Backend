package fr.laucoin.registry.backend.infrastructure.out.api.controller

import fr.laucoin.registry.backend.domain.annotation.RateLimited
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_U
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SEARCH
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SENSITIVE
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CreatedProjectProfilesReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ProjectProfileWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ProjectProfilesWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.UUID

@Deprecated(
	"API v1 has no remaining Registry-Frontend consumer and is scheduled for removal; use the /api/v2 contract.",
	level = DeprecationLevel.WARNING,
)
@Tag(name = "Project's Profiles management (v1, deprecated)", description = "API for Project's Profiles-related operations — deprecated, scheduled for removal; use /api/v2")
@RequestMapping("/api/v1/projects/{projectId}/profiles")
interface IProjectProfileV1Controller {
	@Operation(
		summary = "Find Project's Profiles",
		description = "Find or get paginated Project's Profiles",
		deprecated = true,
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PROFILE_R')")
	@RateLimited(SEARCH, whenParamPresent = ["textSearched"])
	@GetMapping
	fun findProjectProfiles(
		@PathVariable projectId: UUID,
		@RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) pageNumber: Int,
		@RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) pageSize: Int,
		@RequestParam(required = false) textSearched: String?,
		@RequestParam(required = false) availabilitySearched: Boolean?,
		@RequestParam(required = false) statusSearched: ProfileStatusEnum?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) dateTimeSearched: ZonedDateTime?,
	): Mono<PageModel<ProjectProfileReaderDto>>

	@Operation(
		summary = "Find Project's Profile",
		description = "Find Project's Profile by ID",
		deprecated = true,
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
		deprecated = true,
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PROFILE_METADATA_R')")
	@RateLimited(SEARCH, whenParamPresent = ["textSearched"])
	@GetMapping("/search/users")
	fun searchUsers(
		@PathVariable projectId: UUID,
		@RequestParam textSearched: String?,
	): Flux<PartialUserReaderDto>

	@Operation(
		summary = "Get assignable Roles",
		description = "Get all the roles you are allowed to assign",
		deprecated = true,
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
		deprecated = true,
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
		deprecated = true,
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PROFILE_U')")
	@PatchMapping("/{id}")
	fun updateProjectProfile(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid profile: ProjectProfileWriterDto,
	): Mono<ProjectProfileReaderDto>

	@Operation(
		summary = "Block Project's Profile",
		description = "Prproject a User from using it",
		deprecated = true,
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PROFILE_U')")
	@RateLimited(SENSITIVE)
	@PatchMapping("/{id}/block")
	fun blockProjectProfileById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<ProjectProfileReaderDto>

	@Operation(
		summary = "Unblock Project's Profile",
		description = "Re-authorize a User to use it",
		deprecated = true,
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PROFILE_U')")
	@RateLimited(SENSITIVE)
	@PatchMapping("/{id}/unblock")
	fun unblockProjectProfileById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<ProjectProfileReaderDto>

	@Operation(
		summary = "Delete Project's Profile",
		description = "Delete Project's Profile",
		deprecated = true,
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PROFILE_D')")
	@RateLimited(SENSITIVE)
	@DeleteMapping("/{id}")
	fun deleteProjectProfileById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID
	): Mono<Unit>
}
