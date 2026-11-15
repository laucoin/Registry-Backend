package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_U
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.GuestMovementWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat REST des mouvements d'invités sous /api/v2 : création et mise à jour avec leurs nouveaux invités.
 * Périmètre : Porte les routes des mouvements d'invités, leurs règles d'autorisation, leurs bornes de validation et leurs métadonnées OpenAPI.
 * Limites : Ne déclare aucun comportement ; l'implémentation du contrôleur délègue au cas d'usage du mouvement.
 */
@Tag(name = "Movements management", description = "API for Movements-related operations")
@RequestMapping("/api/v2/projects/{projectId}/movements")
interface IMovementGuestV2Controller {
	@Operation(
		summary = "Create Guest Movement",
		description = "Create Movement and related Guest Content",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_C')")
	@PostMapping("/guests")
	fun createGuestsMovement(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@RequestBody @Valid movement: GuestMovementWriterDto,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<MovementReaderDto>>

	@Operation(
		summary = "Update Guest Movement",
		description = "Update Movement and related Guest Content",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_U')")
	@PatchMapping("/guests/{id}")
	fun updateGuestsMovementById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid movement: GuestMovementWriterDto,
		exchange: ServerWebExchange,
	): Mono<MovementReaderDto>
}
