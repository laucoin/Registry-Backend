package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantExportReaderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat REST de l'export des données personnelles d'un participant.
 * Périmètre : Porte la route d'export, son autorisation et ses métadonnées OpenAPI.
 * Limites : Ne déclare aucun comportement ; l'implémentation du contrôleur délègue au cas d'usage.
 */
@Tag(name = "Participants management", description = "API for Participants-related operations")
@RequestMapping("/api/v2/projects/{projectId}/participants")
interface IParticipantExportV2Controller {
	@Operation(
		summary = "Export Participant data",
		description = "Export the personal data of a Participant, limited to its Project. " +
			"Allowed for the linked User or for a Project administrator",
	)
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/{id}/export")
	fun exportParticipantById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		request: ServerHttpRequest,
	): Mono<ResponseEntity<ParticipantExportReaderDto>>
}
