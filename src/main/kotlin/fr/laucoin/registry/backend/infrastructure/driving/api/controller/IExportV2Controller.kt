package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_R
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantExportReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.UserExportReaderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import reactor.core.publisher.Mono

@Tag(name = "Data export", description = "API for GDPR personal data export (JSON)")
@RequestMapping("/api/v2")
interface IExportV2Controller {
	@Operation(
		summary = "Export Current User data",
		description = "Export the personal data of the Current User, including every Participant linked to them",
	)
	@GetMapping("/users/current/export")
	fun exportCurrentUser(@AuthenticationPrincipal currentUser: CurrentUserModel): Mono<ResponseEntity<UserExportReaderDto>>

	@Operation(
		summary = "Export User data",
		description = "Export the personal data of a User, including every Participant linked to them",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_R')")
	@GetMapping("/users/{id}/export")
	fun exportUserById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<ResponseEntity<UserExportReaderDto>>

	@Operation(
		summary = "Export Participant data",
		description = "Export the personal data of a Participant, limited to its Project. " +
			"Allowed for the linked User or for a Project administrator",
	)
	@GetMapping("/projects/{projectId}/participants/{id}/export")
	fun exportParticipantById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<ResponseEntity<ParticipantExportReaderDto>>
}
