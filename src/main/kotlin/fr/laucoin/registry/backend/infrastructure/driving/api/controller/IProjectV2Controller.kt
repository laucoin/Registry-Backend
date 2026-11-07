package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import org.springdoc.core.annotations.ParameterObject
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ProjectQueryDto
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_RESTORE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_SOFT_DELETE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_U
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_PROJECT_C
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectWithProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ProjectWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat REST des endpoints du projet sous /api/v2.
 * Périmètre : Porte les routes, les règles d'autorisation, les bornes de validation et les métadonnées OpenAPI.
 * Limites : Ne déclare aucun comportement ; l'implémentation du contrôleur délègue aux services.
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
		@Valid @ParameterObject search: ProjectQueryDto,
	): Mono<PageModel<ProjectWithProfileReaderDto>>

	@Operation(
		summary = "Find Project",
		description = "Find Project by ID. Only a User with a usable Profile on the Project can read it, whatever their global role.",
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
	@PreAuthorize("hasPermission(#id, '$REGISTRY_PROJECT_SOFT_DELETE')")
	@PostMapping("/{id}/disable")
	fun disableProjectById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<ProjectWithProfileReaderDto>

	@Operation(
		summary = "Enable Project",
		description = "Enable Project, obviously the profiles concerned are accessible again.",
	)
	@PreAuthorize("hasPermission(#id, '$REGISTRY_PROJECT_RESTORE')")
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
