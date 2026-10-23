package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_U
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_PROJECT_C
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SEARCH_QUERY_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SORT_MAX
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectWithProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ProjectWriterDto
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
import reactor.core.publisher.Mono

/**
 * Purpose: REST contract of the project endpoints under /api/v2.
 * Scope: Carries the routes, authorization rules, validation bounds and OpenAPI metadata.
 * Limits: Declares no behavior; the controller implementation delegates to the services.
 */
@Tag(name = "Projects management", description = "API for Projects-related operations")
@RequestMapping("/api/v2/projects")
interface IProjectV2Controller {
	@Operation(
		summary = "Find Projects",
		description = "Find or get paginated Projects with the current User's Profile. Without REGISTRY_PROJECT_R authority, only the Projects with an accepted and currently valid Profile are returned.",
	)
	@PreAuthorize("isAuthenticated()")
	@GetMapping
	fun findProjects(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) page: Int,
		@RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) size: Int,
		@Parameter(description = "Text matched against the project name (accent-insensitive substring)")
		@RequestParam(value = "q", required = false) @Valid @Size(max = SEARCH_QUERY_MAX, message = PARAMETER_TOO_LONG) query: String?,
		@Parameter(description = "Visibility filter: true keeps visible elements only, false keeps hidden ones only, omitted keeps both")
		@RequestParam(required = false) isVisible: Boolean?,
		@Parameter(description = "Favorite filter: true keeps the favorites of the current User only, false keeps the others, omitted keeps both")
		@RequestParam(required = false) isFavorite: Boolean?,
		@Parameter(description = "Profile filter: true keeps the Projects the current User has a valid Profile on, false keeps all the Projects (requires REGISTRY_PROJECT_R authority), omitted defaults to false with the authority and true without")
		@RequestParam(required = false) withProfile: Boolean?,
		@Parameter(description = "Instant that must fall inside the validity window")
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) at: ZonedDateTime?,
		@Parameter(description = "Sort field, prefixed with - for descending order. Supported: name, begin, end, participants, groups, acceptedProfiles, vehicles, activities, activeAlerts, favorite")
		@RequestParam(required = false) @Valid @Size(max = SORT_MAX, message = PARAMETER_TOO_LONG) sort: String?,
	): Mono<PageModel<ProjectWithProfileReaderDto>>

	@Operation(
		summary = "Find Project",
		description = "Find Project by ID",
	)
	@PreAuthorize("hasAuthority('${UserPermissionConst.REGISTRY_PROJECT_R}') || hasPermission(#id, '${ProjectPermissionConst.REGISTRY_PROJECT_R}')")
	@GetMapping("/{id}")
	fun findProjectById(@AuthenticationPrincipal currentUser: CurrentUserModel, @PathVariable id: UUID): Mono<ProjectWithProfileReaderDto>

	@Operation(
		summary = "Create Project",
		description = "Create Project and Project Profile administration for the Current User",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_PROJECT_C')")
	@PostMapping
	fun createProject(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@RequestBody @Valid project: ProjectWriterDto,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<ProjectWithProfileReaderDto>>

	@Operation(
		summary = "Update Project",
		description = "Update Project",
	)
	@PreAuthorize("hasPermission(#id, '$REGISTRY_PROJECT_U')")
	@PatchMapping("/{id}")
	fun updateProjectById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
		@RequestBody @Valid project: ProjectWriterDto,
		exchange: ServerWebExchange,
	): Mono<ProjectWithProfileReaderDto>

	@Operation(
		summary = "Disable Project",
		description = "Disable Project access, obviously the related profile is no accessible anymore.",
	)
	@PreAuthorize("hasPermission(#id, '$REGISTRY_PROJECT_U')")
	@PostMapping("/{id}/disable")
	fun disableProjectById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<ProjectWithProfileReaderDto>

	@Operation(
		summary = "Enable Project",
		description = "Enable Project, obviously the profiles concerned are accessible again.",
	)
	@PreAuthorize("hasPermission(#id, '$REGISTRY_PROJECT_U')")
	@PostMapping("/{id}/enable")
	fun enableProjectById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<ProjectWithProfileReaderDto>

	@Operation(
		summary = "Favorite Project",
		description = "Mark the Project as a favorite of the current User. Idempotent.",
	)
	@PreAuthorize("hasAuthority('${UserPermissionConst.REGISTRY_PROJECT_R}') || hasPermission(#id, '${ProjectPermissionConst.REGISTRY_PROJECT_R}')")
	@PostMapping("/{id}/favorite")
	@ResponseStatus(NO_CONTENT)
	fun addFavoriteProject(@AuthenticationPrincipal currentUser: CurrentUserModel, @PathVariable id: UUID): Mono<Unit>

	@Operation(
		summary = "Unfavorite Project",
		description = "Remove the Project from the favorites of the current User. Idempotent.",
	)
	@PreAuthorize("hasAuthority('${UserPermissionConst.REGISTRY_PROJECT_R}') || hasPermission(#id, '${ProjectPermissionConst.REGISTRY_PROJECT_R}')")
	@DeleteMapping("/{id}/favorite")
	@ResponseStatus(NO_CONTENT)
	fun removeFavoriteProject(@AuthenticationPrincipal currentUser: CurrentUserModel, @PathVariable id: UUID): Mono<Unit>

	@Operation(
		summary = "Delete Project",
		description = "Delete all Project data.",
	)
	@PreAuthorize("hasPermission(#id, '$REGISTRY_PROJECT_D')")
	@DeleteMapping("/{id}")
	@ResponseStatus(NO_CONTENT)
	fun deleteProjectById(@PathVariable id: UUID): Mono<Unit>
}
