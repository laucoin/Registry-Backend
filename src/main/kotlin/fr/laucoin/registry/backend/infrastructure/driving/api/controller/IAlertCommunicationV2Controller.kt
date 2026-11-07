package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import org.springdoc.core.annotations.ParameterObject
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.CommunicationQueryDto
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_COMMUNICATION_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ALERT
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CommunicationReaderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat REST des communications d'une alerte sous /api/v2.
 * Périmètre : Porte la route de lecture paginée des communications liées à une alerte, sa règle d'autorisation, ses bornes de validation et ses métadonnées OpenAPI.
 * Limites : Ne déclare aucun comportement ; l'implémentation du contrôleur délègue au cas d'usage de l'alerte.
 */
@Tag(name = "Alerts management", description = "API for Alerts-related operations")
@RequestMapping("/api/v2/projects/{projectId}/alerts")
interface IAlertCommunicationV2Controller {
	@Operation(
		summary = "Find Alert Communications",
		description = "Find or get paginated alert communications",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ALERT') && hasPermission(#projectId, '$REGISTRY_PROJECT_ALERT_COMMUNICATION_R')")
	@GetMapping("/{id}/communications")
	fun findAlertCommunications(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@Valid @ParameterObject search: CommunicationQueryDto,
	): Mono<PageModel<CommunicationReaderDto>>
}
