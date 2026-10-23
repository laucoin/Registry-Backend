package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_U
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ALERT
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_COMMUNICATION
import fr.laucoin.registry.backend.domain.enumeration.CommunicationTargetEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AlertReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.CommunicationWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Tag(name = "Communications management", description = "API for Communications-related operations")
@RequestMapping("/api/v2/projects/{projectId}/communications")
interface ICommunicationV2Controller {
	@Operation(
		summary = "Find Communications",
		description = "Find or get paginated Communications",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_COMMUNICATION') && hasPermission(#projectId, '$REGISTRY_PROJECT_COMMUNICATION_R')")
	@GetMapping
	fun findCommunications(
		@PathVariable projectId: UUID,
		@RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) page: Int,
		@RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
			200,
			message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
		) size: Int,
		@Parameter(description = "Text matched against the communication message")
		@RequestParam(value = "q", required = false) @Valid @Size(max = 100, message = PARAMETER_TOO_LONG) query: String?,
		@Parameter(description = "Visibility filter: true keeps visible elements only, false keeps hidden ones only, omitted keeps both")
		@RequestParam(required = false) isVisible: Boolean?,
		@Parameter(description = "Start (inclusive) of the period that must contain the element date-time")
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) from: ZonedDateTime?,
		@Parameter(description = "End (inclusive) of the period that must contain the element date-time")
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) to: ZonedDateTime?,
		@Parameter(description = "Kind of element the communication is linked to: a movement, an alert or none of them")
		@RequestParam(required = false) target: CommunicationTargetEnum?,
		@Parameter(description = "Sort field, prefixed with - for descending order. Supported: dateTime, message")
		@RequestParam(required = false) @Valid @Size(max = 32, message = PARAMETER_TOO_LONG) sort: String?,
	): Mono<PageModel<CommunicationReaderDto>>

	@Operation(
		summary = "Find Communication",
		description = "Find Communication by ID",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_COMMUNICATION') && hasPermission(#projectId, '$REGISTRY_PROJECT_COMMUNICATION_R')")
	@GetMapping("/{id}")
	fun findCommunicationById(
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<CommunicationReaderDto>

	@Operation(
		summary = "Search Movements",
		description = "Search Movements with an activity to link a Communication",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_COMMUNICATION') && hasPermission(#projectId, '$REGISTRY_PROJECT_COMMUNICATION_METADATA_R')")
	@GetMapping("/search/movements")
	fun searchMovements(
		@PathVariable projectId: UUID,
		@Parameter(description = "Text matched against the name and description of the activity linked to the movement")
		@RequestParam("q") @Valid @Size(max = 100, message = PARAMETER_TOO_LONG) query: String?
	): Flux<MovementReaderDto>

	@Operation(
		summary = "Search Alerts",
		description = "Search Alerts to link a Communication",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ALERT') && hasPermission(#projectId, '$REGISTRY_PROJECT_COMMUNICATION_METADATA_R')")
	@GetMapping("/search/alerts")
	fun searchAlerts(
		@PathVariable projectId: UUID,
		@Parameter(description = "Text matched against the alert title")
		@RequestParam("q") @Valid @Size(max = 100, message = PARAMETER_TOO_LONG) query: String?
	): Flux<AlertReaderDto>

	@Operation(
		summary = "Create Communication",
		description = "Create Communication linked to the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_COMMUNICATION') && hasPermission(#projectId, '$REGISTRY_PROJECT_COMMUNICATION_C')")
	@PostMapping
	fun createCommunication(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@RequestBody @Valid communication: CommunicationWriterDto,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<CommunicationReaderDto>>

	@Operation(
		summary = "Update Communication",
		description = "Update Communication",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_COMMUNICATION') && hasPermission(#projectId, '$REGISTRY_PROJECT_COMMUNICATION_U')")
	@PatchMapping("/{id}")
	fun updateCommunicationById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid communication: CommunicationWriterDto,
		exchange: ServerWebExchange,
	): Mono<CommunicationReaderDto>

	@Operation(
		summary = "Disable Communication",
		description = "Disable Communication, it will not visible anymore in the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_COMMUNICATION') && hasPermission(#projectId, '$REGISTRY_PROJECT_COMMUNICATION_U')")
	@PostMapping("/{id}/disable")
	fun disableCommunicationById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<CommunicationReaderDto>

	@Operation(
		summary = "Enable Communication",
		description = "Enable Communication, obviously it will be visible again in the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_COMMUNICATION') && hasPermission(#projectId, '$REGISTRY_PROJECT_COMMUNICATION_U')")
	@PostMapping("/{id}/enable")
	fun enableCommunicationById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<CommunicationReaderDto>

	@Operation(
		summary = "Delete Communication",
		description = "Delete all Communication data.",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_COMMUNICATION') && hasPermission(#projectId, '$REGISTRY_PROJECT_COMMUNICATION_D')")
	@DeleteMapping("/{id}")
	@ResponseStatus(NO_CONTENT)
	fun deleteCommunicationById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<Unit>
}
