package fr.laucoin.registry.backend.infrastructure.out.api.controller

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_COMMUNICATION_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_U
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ALERT
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.AlertReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.AlertCreationWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.AlertWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Mono

@Tag(name = "Alerts management", description = "API for Alerts-related operations")
@RequestMapping("/api/v1/projects/{projectId}/alerts")
interface IAlertV1Controller {
	@Operation(
		summary = "Find Alerts",
		description = "Find or get paginated Alerts",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ALERT') && hasPermission(#projectId, '$REGISTRY_PROJECT_ALERT_R')")
	@GetMapping
	fun findAlerts(
		@PathVariable projectId: UUID,
		@RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) pageNumber: Int,
		@RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) pageSize: Int,
		@RequestParam(required = false) textSearched: String?,
		@RequestParam(required = false) visibilitySearched: Boolean?,
		@RequestParam(required = false) statusSearched: AlertStatusEnum?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) startDateTimeSearched: ZonedDateTime?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) endDateTimeSearched: ZonedDateTime?,
	): Mono<PageModel<AlertReaderDto>>

	@Operation(
		summary = "Find Alert",
		description = "Find Alert by ID",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ALERT') && hasPermission(#projectId, '$REGISTRY_PROJECT_ALERT_R')")
	@GetMapping("/{id}")
	fun findAlertById(
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<AlertReaderDto>

	@Operation(
		summary = "Find Alert Communications",
		description = "Find or get paginated alert communications",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ALERT') && hasPermission(#projectId, '$REGISTRY_PROJECT_ALERT_COMMUNICATION_R')")
	@GetMapping("/{id}/communications")
	fun findAlertCommunications(
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) pageNumber: Int,
		@RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) pageSize: Int,
		@RequestParam(required = false) textSearched: String?,
		@RequestParam(required = false) visibilitySearched: Boolean?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) startDateTimeSearched: ZonedDateTime?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) endDateTimeSearched: ZonedDateTime?,
	): Mono<PageModel<CommunicationReaderDto>>

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
	): Mono<AlertReaderDto>

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
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ALERT') && hasPermission(#projectId, '$REGISTRY_PROJECT_ALERT_U')")
	@PatchMapping("/{id}/disable")
	fun disableAlertById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<AlertReaderDto>

	@Operation(
		summary = "Enable Alert",
		description = "Enable Alert, obviously it will be visible again in the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ALERT') && hasPermission(#projectId, '$REGISTRY_PROJECT_ALERT_U')")
	@PatchMapping("/{id}/enable")
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
	fun deleteAlertById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<Unit>
}
