package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.annotation.RateLimited
import fr.laucoin.registry.backend.domain.constant.ApiConst.API_V2
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_U
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_PROJECT_C
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_PROJECT_METADATA_R
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SEARCH
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SENSITIVE
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.SortedPageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PageReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectOptionsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ProjectWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
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
		description = "Find or get paginated Projects",
	)
	@RateLimited(SEARCH, whenParamPresent = ["q"])
	@GetMapping
	fun findProjects(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@ParameterObject @Valid page: SortedPageQueryDto,
		@RequestParam(required = false) q: String?,
		@RequestParam(required = false) visible: Boolean?,
		@Parameter(description = "\"false\" value will be considered only if you have REGISTRY_PROJECT_R authority.")
		@RequestParam(required = false, defaultValue = "true") withProfile: Boolean,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) dateTime: ZonedDateTime?,
		@Parameter(description = "Only relevant with \"withProfile\" true, since it filters on the caller's own Profile.")
		@RequestParam(required = false) favorite: Boolean?,
	): Mono<PageReaderDto<ProjectReaderDto>>

	@Operation(
		summary = "Find Project",
		description = "Find Project by ID",
	)
	@PreAuthorize("hasAuthority('${UserPermissionConst.REGISTRY_PROJECT_R}') || hasPermission(#id, '${ProjectPermissionConst.REGISTRY_PROJECT_R}')")
	@GetMapping("/{id}")
	fun findProjectById(@PathVariable id: UUID): Mono<ProjectReaderDto>

	@Operation(
		summary = "Get available Options",
		description = "Get all the Options you are allowed to enable",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_PROJECT_METADATA_R')")
	@GetMapping("/options")
	fun getAvailableProjectOptions(): Flux<ProjectOptionsReaderDto>

	@Operation(
		summary = "Create Project",
		description = "Create Project and Project Profile administration for the Current User",
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
		description = "Update Project",
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
		description = "Disable Project access, obviously the related profile is no accessible anymore.",
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
		description = "Enable Project, obviously the profiles concerned are accessible again.",
	)
	@PreAuthorize("hasPermission(#id, '$REGISTRY_PROJECT_U')")
	@RateLimited(SENSITIVE)
	@PostMapping("/{id}/enable")
	fun enableProjectById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<ProjectReaderDto>

	@Operation(
		summary = "Delete Project",
		description = "Delete all Project data.",
	)
	@PreAuthorize("hasPermission(#id, '$REGISTRY_PROJECT_D')")
	@RateLimited(SENSITIVE)
	@ResponseStatus(NO_CONTENT)
	@DeleteMapping("/{id}")
	fun deleteProjectById(@PathVariable id: UUID): Mono<Unit>
}
