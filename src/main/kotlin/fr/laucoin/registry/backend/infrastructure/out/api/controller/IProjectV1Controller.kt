package fr.laucoin.registry.backend.infrastructure.out.api.controller

import fr.laucoin.registry.backend.domain.annotation.RateLimited
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_U
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_PROJECT_C
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_PROJECT_METADATA_R
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SEARCH
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SENSITIVE
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectOptionsReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ProjectWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
@Tag(name = "Projects management (v1, deprecated)", description = "API for Projects-related operations — deprecated, scheduled for removal; use /api/v2")
@RequestMapping("/api/v1/projects")
interface IProjectV1Controller {
	@Operation(
		summary = "Find Projects",
		description = "Find or get paginated Projects",
		deprecated = true,
	)
	@RateLimited(SEARCH, whenParamPresent = ["textSearched"])
	@GetMapping
	fun findProjects(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) pageNumber: Int,
		@RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) pageSize: Int,
		@RequestParam(required = false) textSearched: String?,
		@RequestParam(required = false) visibilitySearched: Boolean?,
		@Parameter(description = "\"false\" value will be considered only if you have REGISTRY_PROJECT_R authority.")
		@RequestParam(required = false, defaultValue = "true") withProfile: Boolean,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) dateTimeSearched: ZonedDateTime?,
	): Mono<PageModel<ProjectReaderDto>>

	@Operation(
		summary = "Find Project",
		description = "Find Project by ID",
		deprecated = true,
	)
	@PreAuthorize("hasAuthority('${UserPermissionConst.REGISTRY_PROJECT_R}') || hasPermission(#id, '${ProjectPermissionConst.REGISTRY_PROJECT_R}')")
	@GetMapping("/{id}")
	fun findProjectById(@PathVariable id: UUID): Mono<ProjectReaderDto>

	@Operation(
		summary = "Get available Options",
		description = "Get all the Options you are allowed to enable",
		deprecated = true,
	)
	@PreAuthorize("hasAuthority('$REGISTRY_PROJECT_METADATA_R')")
	@GetMapping("/options")
	fun getAvailableProjectOptions(): Flux<ProjectOptionsReaderDto>

	@Operation(
		summary = "Create Project",
		description = "Create Project and Project Profile administration for the Current User",
		deprecated = true,
	)
	@PreAuthorize("hasAuthority('$REGISTRY_PROJECT_C')")
	@PostMapping
	fun createProject(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@RequestBody @Valid project: ProjectWriterDto,
	): Mono<ProjectReaderDto>

	@Operation(
		summary = "Update Project",
		description = "Update Project",
		deprecated = true,
	)
	@PreAuthorize("hasPermission(#id, '$REGISTRY_PROJECT_U')")
	@PatchMapping("/{id}")
	fun updateProjectById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
		@RequestBody @Valid project: ProjectWriterDto
	): Mono<ProjectReaderDto>

	@Operation(
		summary = "Disable Project",
		description = "Disable Project access, obviously the related profile is no accessible anymore.",
		deprecated = true,
	)
	@PreAuthorize("hasPermission(#id, '$REGISTRY_PROJECT_U')")
	@RateLimited(SENSITIVE)
	@PatchMapping("/{id}/disable")
	fun disableProjectById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<ProjectReaderDto>

	@Operation(
		summary = "Enable Project",
		description = "Enable Project, obviously the profiles concerned are accessible again.",
		deprecated = true,
	)
	@PreAuthorize("hasPermission(#id, '$REGISTRY_PROJECT_U')")
	@RateLimited(SENSITIVE)
	@PatchMapping("/{id}/enable")
	fun enableProjectById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<ProjectReaderDto>

	@Operation(
		summary = "Delete Project",
		description = "Delete all Project data.",
		deprecated = true,
	)
	@PreAuthorize("hasPermission(#id, '$REGISTRY_PROJECT_D')")
	@RateLimited(SENSITIVE)
	@DeleteMapping("/{id}")
	fun deleteProjectById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<Unit>
}
