package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SEARCH_QUERY_MAX
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PartialUserReaderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Flux

/**
 * Objectif : Contrat REST des métadonnées du profil de projet : recherche des utilisateurs à inviter et rôles assignables.
 * Périmètre : Porte les routes de lecture ciblée, leurs autorisations, leurs bornes de validation et leurs métadonnées OpenAPI.
 * Limites : Ne déclare aucun comportement ; l'implémentation du contrôleur délègue au cas d'usage.
 */
@Tag(name = "Project's Profiles management", description = "API for Project's Profiles-related operations")
@RequestMapping("/api/v2/projects/{projectId}/profiles")
interface IProjectProfileMetadataV2Controller {
	@Operation(
		summary = "Search Users",
		description = "Find the User to invite to a Project from their exact email address",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PROFILE_METADATA_R')")
	@GetMapping("/search/users")
	fun searchUsers(
		@PathVariable projectId: UUID,
		@Parameter(description = "Exact email address of the user (case-insensitive); partial text matches nothing")
		@RequestParam("q") @Valid @Size(max = SEARCH_QUERY_MAX, message = PARAMETER_TOO_LONG) query: String?,
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
}
