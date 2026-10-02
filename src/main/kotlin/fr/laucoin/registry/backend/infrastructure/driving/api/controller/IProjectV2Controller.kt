package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.annotation.RateLimited
import fr.laucoin.registry.backend.domain.constant.ApiConst.API_V2
import fr.laucoin.registry.backend.domain.constant.ApiConst.DEFAULT_DASHBOARD_LIMIT
import fr.laucoin.registry.backend.domain.constant.ApiConst.MAX_DASHBOARD_LIMIT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_EXCEEDS_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_U
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_PROJECT_C
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SEARCH
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SENSITIVE
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.SortedPageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PageReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ProjectWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springdoc.core.annotations.ParameterObject
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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.UUID

@Tag(name = "Projects management", description = "API for Projects-related operations")
@RequestMapping("$API_V2/projects")
interface IProjectV2Controller {
	@Operation(
		summary = "Find Projects",
		description = """
			Search and list Projects, with pagination and sorting. Combine `q` (free-text search), `visible`,
			`withProfile` (only Projects the caller has a Profile on, true by default) and `dateTime` to narrow the results.
			`favorite` filters on the caller's own Profile and is only meaningful together with `withProfile=true`.
		""",
	)
	@RateLimited(SEARCH, whenParamPresent = ["q"])
	@GetMapping
	fun findProjects(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@ParameterObject @Valid page: SortedPageQueryDto,
		@RequestParam(name = "q", required = false) query: String?,
		@RequestParam(name = "visible", required = false) isVisible: Boolean?,
		@Parameter(description = "\"false\" value will be considered only if you have REGISTRY_PROJECT_R authority.")
		@RequestParam(required = false, defaultValue = "true") withProfile: Boolean,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) dateTime: ZonedDateTime?,
		@Parameter(description = "Only relevant with \"withProfile\" true, since it filters on the caller's own Profile.")
		@RequestParam(name = "favorite", required = false) isFavorite: Boolean?,
	): Mono<PageReaderDto<ProjectReaderDto>>

	@Operation(
		summary = "Find Projects requiring attention",
		description = """
			Dashboard widget: the caller's ACCEPTED Projects that are still in progress (no end date, or an end
			date/time that hasn't passed yet) and currently have at least one ongoing (IN_PROGRESS) Alert, sorted by that
			count descending. Each row carries its counts and the caller's own `activeProfile`. Results are capped at
			"limit" rows (default $DEFAULT_DASHBOARD_LIMIT, max $MAX_DASHBOARD_LIMIT).
		""",
	)
	@GetMapping("/attention")
	fun findProjectsRequiringAttention(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@RequestParam(defaultValue = DEFAULT_DASHBOARD_LIMIT)
		@Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
			MAX_DASHBOARD_LIMIT,
			message = PAGE_SIZE_EXCEEDS_MAX_PAGE_SIZE
		)
		limit: Int,
	): Flux<ProjectReaderDto>

	@Operation(
		summary = "Find Project",
		description = "Get a single Project by its ID.",
	)
	@PreAuthorize("hasAuthority('${UserPermissionConst.REGISTRY_PROJECT_R}') || hasPermission(#id, '${ProjectPermissionConst.REGISTRY_PROJECT_R}')")
	@GetMapping("/{id}")
	fun findProjectById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<ProjectReaderDto>

	@Operation(
		summary = "Create Project",
		description = """
			Create a new Project with its schedule and enabled options (VEHICLE, ACTIVITY, COMMUNICATION, ALERT; note
			some options require others, e.g. ALERT requires ACTIVITY and COMMUNICATION), and grant the caller an administration Profile on it.
		""",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_PROJECT_C')")
	@RateLimited(SENSITIVE)
	@PostMapping
	fun createProject(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@RequestBody @Valid project: ProjectWriterDto,
	): Mono<ResponseEntity<ProjectReaderDto>>

	@Operation(
		summary = "Update Project",
		description = "Update the Project's name, schedule and enabled options (same shape as creation).",
	)
	@PreAuthorize("hasPermission(#id, '$REGISTRY_PROJECT_U')")
	@RateLimited(SENSITIVE)
	@PatchMapping("/{id}")
	fun updateProjectById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
		@RequestBody @Valid project: ProjectWriterDto,
	): Mono<ProjectReaderDto>

	@Operation(
		summary = "Disable Project",
		description = "Soft-delete the Project: it is kept but no longer accessible, and every Profile on it loses access.",
	)
	@PreAuthorize("hasPermission(#id, '$REGISTRY_PROJECT_U')")
	@RateLimited(SENSITIVE)
	@PostMapping("/{id}/disable")
	fun disableProjectById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<ProjectReaderDto>

	@Operation(
		summary = "Enable Project",
		description = "Reverse a disable: the Project and the Profiles on it become accessible again.",
	)
	@PreAuthorize("hasPermission(#id, '$REGISTRY_PROJECT_D')")
	@RateLimited(SENSITIVE)
	@PostMapping("/{id}/enable")
	fun enableProjectById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<ProjectReaderDto>

	@Operation(
		summary = "Delete Project",
		description = """
			Permanently delete the Project and all its data (configuration, Profiles, content). This cannot be undone;
			prefer disabling it if it may be needed again.
		""",
	)
	@PreAuthorize("hasPermission(#id, '$REGISTRY_PROJECT_D')")
	@RateLimited(SENSITIVE)
	@ResponseStatus(NO_CONTENT)
	@DeleteMapping("/{id}")
	fun deleteProjectById(@PathVariable id: UUID): Mono<Unit>
}
