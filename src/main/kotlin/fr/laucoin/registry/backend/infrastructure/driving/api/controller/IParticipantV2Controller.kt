package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import org.springdoc.core.annotations.ParameterObject
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ParticipantQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ElementMovementQueryDto
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_HISTORY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_RESTORE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_SOFT_DELETE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_U
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantTimelineReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ParticipantWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.TimeZone
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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat REST des endpoints du participant sous /api/v2.
 * Périmètre : Porte les routes, les règles d'autorisation, les bornes de validation et les métadonnées OpenAPI.
 * Limites : Ne déclare aucun comportement ; l'implémentation du contrôleur délègue aux services.
 */
@Tag(name = "Participants management", description = "API for Participants-related operations")
@RequestMapping("/api/v2/projects/{projectId}/participants")
interface IParticipantV2Controller {
	@Operation(
		summary = "Find Participants",
		description = "Find or get paginated Participants",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_R')")
	@GetMapping
	fun findParticipants(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@Valid @ParameterObject search: ParticipantQueryDto,
	): Mono<PageModel<ParticipantReaderDto>>

	@Operation(
		summary = "Find Birthdays",
		description = "Find the Participants whose birthday is today",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_R')")
	@GetMapping("/birthday")
	fun findBirthdays(
		@PathVariable projectId: UUID,
	): Flux<ParticipantReaderDto>

	@Operation(
		summary = "Find Participant",
		description = "Find Participant by ID",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_R')")
	@GetMapping("/{id}")
	fun findParticipantById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<ParticipantReaderDto>

	@Operation(
		summary = "Find Participant timeline",
		description = "Planned presence (the availability window, empty when none is set; a missing bound is open) and actual outings of the Participant. An outing goes from an OUT movement to the next IN one and is flagged ongoing, ending now, when the Participant has not come back. A definitive departure is not an outing. Guests have no outings",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_HISTORY_R')")
	@GetMapping("/{id}/timeline")
	fun findParticipantTimeline(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<ParticipantTimelineReaderDto>

	@Operation(
		summary = "Find Participant Movements",
		description = "Find or get paginated participant Movements",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_HISTORY_R')")
	@GetMapping("/{id}/movements")
	fun findParticipantMovements(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@Valid @ParameterObject search: ElementMovementQueryDto,
	): Mono<PageModel<MovementReaderDto>>

	@Operation(
		summary = "Create Participant",
		description = "Create Participant linked to the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_C')")
	@PostMapping
	fun createParticipant(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@RequestBody @Valid participant: ParticipantWriterDto,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<ParticipantReaderDto>>

	@Operation(
		summary = "Update Participant",
		description = "Update Participant",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_U')")
	@PatchMapping("/{id}")
	fun updateParticipantById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		timeZone: TimeZone,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid participant: ParticipantWriterDto,
		exchange: ServerWebExchange,
	): Mono<ParticipantReaderDto>

	@Operation(
		summary = "Disable Participant",
		description = "Disable Participant, it will not visible anymore in the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_SOFT_DELETE')")
	@PostMapping("/{id}/disable")
	fun disableParticipantById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<ParticipantReaderDto>

	@Operation(
		summary = "Enable Participant",
		description = "Enable Participant, obviously it will be visible again in the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_RESTORE')")
	@PostMapping("/{id}/enable")
	fun enableParticipantById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<ParticipantReaderDto>

	@Operation(
		summary = "Delete Participant",
		description = "Delete all Participant data.",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_D')")
	@DeleteMapping("/{id}")
	@ResponseStatus(NO_CONTENT)
	fun deleteParticipantById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<Unit>
}
