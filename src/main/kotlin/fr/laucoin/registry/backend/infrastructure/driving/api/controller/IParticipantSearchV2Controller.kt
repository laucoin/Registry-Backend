package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SEARCH_QUERY_MAX
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.GroupWithoutMemberReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PartialUserReaderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Flux

/**
 * Objectif : Contrat REST de la recherche des utilisateurs et des groupes à lier à un participant.
 * Périmètre : Porte les routes de recherche, leurs autorisations, leurs bornes de validation et leurs métadonnées OpenAPI.
 * Limites : Ne déclare aucun comportement ; l'implémentation du contrôleur délègue au cas d'usage.
 */
@Tag(name = "Participants management", description = "API for Participants-related operations")
@RequestMapping("/api/v2/projects/{projectId}/participants")
interface IParticipantSearchV2Controller {
	@Operation(
		summary = "Search Users",
		description = "Search Users to link to a Participant",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_METADATA_R')")
	@GetMapping("/search/users")
	fun searchUsers(
		@PathVariable projectId: UUID,
		@Parameter(description = "Text matched against the user first name, last name and email")
		@RequestParam("q") @Valid @Size(max = SEARCH_QUERY_MAX, message = PARAMETER_TOO_LONG) query: String?,
	): Flux<PartialUserReaderDto>

	@Operation(
		summary = "Search Groups",
		description = "Search Groups to add Participant in it",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_METADATA_R')")
	@GetMapping("/search/groups")
	fun searchGroups(
		@PathVariable projectId: UUID,
		@Parameter(description = "Text matched against the group name (accent-insensitive substring)")
		@RequestParam("q") @Valid @Size(max = SEARCH_QUERY_MAX, message = PARAMETER_TOO_LONG) query: String?
	): Flux<GroupWithoutMemberReaderDto>
}
