package fr.laucoin.registry.backend.infrastructure.out.api.controller

import fr.laucoin.registry.backend.domain.annotation.RateLimited
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
import org.springdoc.core.annotations.ParameterObject
import fr.laucoin.registry.backend.infrastructure.out.api.dto.PageQueryDto
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

@Tag(name = "Project's Profiles management (v2)", description = "API for Project's Profiles-related operations")
@RequestMapping("/api/v2/projects/{projectId}/profiles")
interface IProjectProfileV2Controller {
	@Operation(
		summary = "Find Project's Profiles",
		description = "Paginated Project's Profiles",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PROFILE_R')")
	@RateLimited(SEARCH, whenParamPresent = ["q"])
	@GetMapping
	fun findProjectProfiles(
		@PathVariable projectId: UUID,
		@ParameterObject @Valid pageQuery: PageQueryDto,
		@RequestParam(required = false) q: String?,
		@RequestParam(required = false) available: Boolean?,
		@RequestParam(required = false) status: ProfileStatusEnum?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) dateTime: ZonedDateTime?,
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
		summary = "Search assignable Users",
		description = "Search Users assignable to this Project (eligibility sub-collection)",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PROFILE_METADATA_R')")
	@RateLimited(SEARCH)
	@GetMapping("/assignable-users")
	fun searchAssignableUsers(
		@PathVariable projectId: UUID,
		@RequestParam(required = false) q: String?,
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
		description = "Update Project's Profile role / access window",
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
		description = "Prevent a User from using it",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PROFILE_U')")
	@RateLimited(SENSITIVE)
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
	@RateLimited(SENSITIVE)
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
	@RateLimited(SENSITIVE)
	@DeleteMapping("/{id}")
	fun deleteProjectProfileById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID
	): Mono<Unit>
}
