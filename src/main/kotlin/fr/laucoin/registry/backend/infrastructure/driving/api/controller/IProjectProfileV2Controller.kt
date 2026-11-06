package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import org.springdoc.core.annotations.ParameterObject
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ProjectProfileQueryDto
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_RESTORE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_SOFT_DELETE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_U
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CreatedProjectProfilesReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ProjectProfileWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ProjectProfilesWriterDto
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
 * Objectif : Contrat REST des endpoints du profil de projet sous /api/v2.
 * Périmètre : Porte les routes, les règles d'autorisation, les bornes de validation et les métadonnées OpenAPI.
 * Limites : Ne déclare aucun comportement ; l'implémentation du contrôleur délègue aux services.
 */
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
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@Valid @ParameterObject search: ProjectProfileQueryDto,
	): Mono<PageModel<ProjectProfileReaderDto>>

	@Operation(
		summary = "Find Project's Profile",
		description = "Find Project's Profile by ID",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PROFILE_R')")
	@GetMapping("/{id}")
	fun findProjectProfileById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<ProjectProfileReaderDto>

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
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PROFILE_SOFT_DELETE')")
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
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PROFILE_RESTORE')")
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
