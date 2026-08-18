package fr.laucoin.registry.backend.infrastructure.out.api.controller

import fr.laucoin.registry.backend.domain.annotation.RateLimited
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_U
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_COMMUNICATION
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SEARCH
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SENSITIVE
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.AlertReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.CommunicationWriterDto
import org.springdoc.core.annotations.ParameterObject
import fr.laucoin.registry.backend.infrastructure.out.api.dto.SortedPageQueryDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.UUID

@Tag(name = "Communications management (v2)", description = "API for Communications-related operations")
@RequestMapping("/api/v2/projects/{projectId}/communications")
interface ICommunicationV2Controller {
	@Operation(
		summary = "Find Communications",
		description = "Paginated Communications",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_COMMUNICATION') && hasPermission(#projectId, '$REGISTRY_PROJECT_COMMUNICATION_R')")
	@RateLimited(SEARCH, whenParamPresent = ["q"])
	@GetMapping
	fun findCommunications(
		@PathVariable projectId: UUID,
		@ParameterObject @Valid pageQuery: SortedPageQueryDto,
		@RequestParam(required = false) q: String?,
		@RequestParam(required = false) visible: Boolean?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) startDateTime: ZonedDateTime?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) endDateTime: ZonedDateTime?,
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
		summary = "Get attachable Movements",
		description = "Movements (with an activity) a Communication can be attached to (eligibility sub-collection)",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_COMMUNICATION_METADATA_R')")
	@RateLimited(SEARCH)
	@GetMapping("/attachable-movements")
	fun getAttachableMovements(
		@PathVariable projectId: UUID,
		@RequestParam(required = false) q: String?,
	): Flux<MovementReaderDto>

	@Operation(
		summary = "Get attachable Alerts",
		description = "Alerts a Communication can be attached to (eligibility sub-collection)",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_COMMUNICATION_METADATA_R')")
	@RateLimited(SEARCH)
	@GetMapping("/attachable-alerts")
	fun getAttachableAlerts(
		@PathVariable projectId: UUID,
		@RequestParam(required = false) q: String?,
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
	): Mono<CommunicationReaderDto>

	@Operation(
		summary = "Update Communication",
		description = "Update a Communication's editable fields",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_COMMUNICATION') && hasPermission(#projectId, '$REGISTRY_PROJECT_COMMUNICATION_U')")
	@PatchMapping("/{id}")
	fun updateCommunicationById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid communication: CommunicationWriterDto,
	): Mono<CommunicationReaderDto>

	@Operation(
		summary = "Disable Communication",
		description = "Disable Communication, it will not be visible anymore in the Project",
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
		description = "Enable Communication, it will be visible again in the Project",
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
	@DeleteMapping("/{id}")
	fun deleteCommunicationById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<Unit>
}
