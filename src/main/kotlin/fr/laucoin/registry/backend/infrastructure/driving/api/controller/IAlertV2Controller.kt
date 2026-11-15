package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import org.springdoc.core.annotations.ParameterObject
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.AlertQueryDto
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_RESTORE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_SOFT_DELETE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_U
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ALERT
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AlertReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AlertStatisticsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.AlertCreationWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.AlertWriterDto
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
 * Objectif : Contrat REST des endpoints de l'alerte sous /api/v2.
 * Périmètre : Porte les routes, les règles d'autorisation, les bornes de validation et les métadonnées OpenAPI.
 * Limites : Ne déclare aucun comportement ; l'implémentation du contrôleur délègue aux services.
 */
@Tag(name = "Alerts management", description = "API for Alerts-related operations")
@RequestMapping("/api/v2/projects/{projectId}/alerts")
interface IAlertV2Controller {
	@Operation(
		summary = "Find Alerts",
		description = "Find or get paginated Alerts. The last communication is returned only to callers allowed to read the alert communications",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ALERT') && hasPermission(#projectId, '$REGISTRY_PROJECT_ALERT_R')")
	@GetMapping
	fun findAlerts(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@Valid @ParameterObject search: AlertQueryDto,
	): Mono<PageModel<AlertReaderDto>>

	@Operation(
		summary = "Find Alert statistics",
		description = "Statistics of the visible alerts of the Project: resolved and canceled counts, average resolution time in seconds (resolved alerts only) and real alert rate (resolved over resolved plus canceled). Rates and averages are null when there is nothing to compute them from",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ALERT') && hasPermission(#projectId, '$REGISTRY_PROJECT_ALERT_R')")
	@GetMapping("/statistics")
	fun findAlertStatistics(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
	): Mono<AlertStatisticsReaderDto>

	@Operation(
		summary = "Find Alert",
		description = "Find Alert by ID. The last communication is returned only to callers allowed to read the alert communications",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ALERT') && hasPermission(#projectId, '$REGISTRY_PROJECT_ALERT_R')")
	@GetMapping("/{id}")
	fun findAlertById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<AlertReaderDto>

	@Operation(
		summary = "Create Alert",
		description = "Create Alert linked to the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ALERT') && hasPermission(#projectId, '$REGISTRY_PROJECT_ALERT_C')")
	@PostMapping
	fun createAlert(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@RequestBody @Valid alert: AlertCreationWriterDto,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<AlertReaderDto>>

	@Operation(
		summary = "Update Alert",
		description = "Update Alert",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ALERT') && hasPermission(#projectId, '$REGISTRY_PROJECT_ALERT_U')")
	@PatchMapping("/{id}")
	fun updateAlertById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid alert: AlertWriterDto,
		exchange: ServerWebExchange,
	): Mono<AlertReaderDto>

	@Operation(
		summary = "Update Alert status",
		description = "Update Alert status",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ALERT') && hasPermission(#projectId, '$REGISTRY_PROJECT_ALERT_U')")
	@PatchMapping("/{id}/status/{status}")
	fun updateAlertStatusById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@PathVariable status: AlertStatusEnum,
	): Mono<AlertReaderDto>

	@Operation(
		summary = "Disable Alert",
		description = "Disable Alert, it will not visible anymore in the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ALERT') && hasPermission(#projectId, '$REGISTRY_PROJECT_ALERT_SOFT_DELETE')")
	@PostMapping("/{id}/disable")
	fun disableAlertById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<AlertReaderDto>

	@Operation(
		summary = "Enable Alert",
		description = "Enable Alert, obviously it will be visible again in the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ALERT') && hasPermission(#projectId, '$REGISTRY_PROJECT_ALERT_RESTORE')")
	@PostMapping("/{id}/enable")
	fun enableAlertById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<AlertReaderDto>

	@Operation(
		summary = "Delete Alert",
		description = "Delete all Alert data.",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ALERT') && hasPermission(#projectId, '$REGISTRY_PROJECT_ALERT_D')")
	@DeleteMapping("/{id}")
	@ResponseStatus(NO_CONTENT)
	fun deleteAlertById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<Unit>
}
