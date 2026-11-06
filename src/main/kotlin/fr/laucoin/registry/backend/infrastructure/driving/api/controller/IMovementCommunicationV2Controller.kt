package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_COMMUNICATION_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_COMMUNICATION
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.CommunicationQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CommunicationReaderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
import org.springdoc.core.annotations.ParameterObject
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat REST des communications d'un mouvement sous /api/v2.
 * Périmètre : Porte la route de lecture paginée des communications liées à un mouvement, sa règle d'autorisation, ses bornes de validation et ses métadonnées OpenAPI.
 * Limites : Ne déclare aucun comportement ; l'implémentation du contrôleur délègue au cas d'usage du mouvement.
 */
@Tag(name = "Movements management", description = "API for Movements-related operations")
@RequestMapping("/api/v2/projects/{projectId}/movements")
interface IMovementCommunicationV2Controller {
	@Operation(
		summary = "Find Movements Communications",
		description = "Find or get paginated movement communications",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_COMMUNICATION') && hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_COMMUNICATION_R')")
	@GetMapping("/{id}/communications")
	fun findMovementCommunications(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@Valid @ParameterObject search: CommunicationQueryDto,
	): Mono<PageModel<CommunicationReaderDto>>
}
