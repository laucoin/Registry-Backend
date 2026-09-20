package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.annotation.RateLimited
import fr.laucoin.registry.backend.domain.constant.ApiConst.API_V2
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_U
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ALERT
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_COMMUNICATION
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SEARCH
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SENSITIVE
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.SortedPageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AlertReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PageReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.CommunicationWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.UUID

@Tag(name = "Communications management", description = "API for Communications-related operations")
@RequestMapping("$API_V2/projects/{projectId}/communications")
interface ICommunicationV2Controller {
	@Operation(
		summary = "Find Communications",
		description = "Find or get paginated Communications",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_COMMUNICATION') && hasPermission(#projectId, '$REGISTRY_PROJECT_COMMUNICATION_R')")
	@RateLimited(SEARCH, whenParamPresent = ["q"])
	@GetMapping
	fun findCommunications(
		@PathVariable projectId: UUID,
		@ParameterObject @Valid page: SortedPageQueryDto,
		@RequestParam(required = false) q: String?,
		@RequestParam(required = false) visible: Boolean?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) startDateTime: ZonedDateTime?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) endDateTime: ZonedDateTime?,
	): Mono<PageReaderDto<CommunicationReaderDto>>

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
	@RateLimited(SEARCH)
	@GetMapping("/search/movements")
	fun searchActivities(
		@PathVariable projectId: UUID,
		@RequestParam q: String?,
	): Flux<MovementReaderDto>

	@Operation(
		summary = "Search Alerts",
		description = "Search Alerts to link a Communication",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ALERT') && hasPermission(#projectId, '$REGISTRY_PROJECT_COMMUNICATION_METADATA_R')")
	@RateLimited(SEARCH)
	@GetMapping("/search/alerts")
	fun searchAlerts(
		@PathVariable projectId: UUID,
		@RequestParam q: String?,
	): Flux<AlertReaderDto>

	@Operation(
		summary = "Create Communication",
		description = "Create Communication linked to the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_COMMUNICATION') && hasPermission(#projectId, '$REGISTRY_PROJECT_COMMUNICATION_C')")
	@RateLimited(SENSITIVE)
	@PostMapping
	fun createCommunication(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@RequestBody @Valid communication: CommunicationWriterDto,
	): Mono<ResponseEntity<CommunicationReaderDto>>

	@Operation(
		summary = "Update Communication",
		description = "Update Communication",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_COMMUNICATION') && hasPermission(#projectId, '$REGISTRY_PROJECT_COMMUNICATION_U')")
	@RateLimited(SENSITIVE)
	@PatchMapping("/{id}")
	fun updateCommunicationById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid communication: CommunicationWriterDto,
	): Mono<CommunicationReaderDto>

	@Operation(
		summary = "Disable Communication",
		description = "Disable Communication, it will not visible anymore in the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_COMMUNICATION') && hasPermission(#projectId, '$REGISTRY_PROJECT_COMMUNICATION_U')")
	@RateLimited(SENSITIVE)
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
	@RateLimited(SENSITIVE)
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
	@RateLimited(SENSITIVE)
	@ResponseStatus(NO_CONTENT)
	@DeleteMapping("/{id}")
	fun deleteCommunicationById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<Unit>
}
