package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.annotation.RateLimited
import fr.laucoin.registry.backend.domain.constant.ApiConst.API_V2
import fr.laucoin.registry.backend.domain.constant.ApiConst.DEFAULT_DASHBOARD_LIMIT
import fr.laucoin.registry.backend.domain.constant.ApiConst.MAX_DASHBOARD_LIMIT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_EXCEEDS_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_COMMUNICATION_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_U
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ALERT
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SEARCH
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SENSITIVE
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.DateTimeRangeQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.PageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.SortedPageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AlertReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.OngoingAlertReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PageReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.AlertCreationWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.AlertStatusWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.AlertWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springdoc.core.annotations.ParameterObject
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
import java.util.UUID

@Tag(name = "Alerts management", description = "API for Alerts-related operations")
@RequestMapping("$API_V2/projects/{projectId}/alerts")
interface IAlertV2Controller {
	@Operation(
		summary = "Find Alerts",
		description = """
			Search and list the Project's Alerts, with pagination and sorting. Combine `q` (free-text search),
			`visible`, `status` (IN_PROGRESS / RESOLVED / CANCELED) and a `startDateTime`/`endDateTime` range to narrow the results.
		""",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ALERT') && hasPermission(#projectId, '$REGISTRY_PROJECT_ALERT_R')")
	@RateLimited(SEARCH, whenParamPresent = ["q"])
	@GetMapping
	fun findAlerts(
		@PathVariable projectId: UUID,
		@ParameterObject @Valid page: SortedPageQueryDto,
		@RequestParam(name = "q", required = false) query: String?,
		@RequestParam(name = "visible", required = false) isVisible: Boolean?,
		@RequestParam(required = false) status: AlertStatusEnum?,
		@ParameterObject dateTimeRange: DateTimeRangeQueryDto,
	): Mono<PageReaderDto<AlertReaderDto>>

	@Operation(
		summary = "Find Alert",
		description = "Get a single Alert of the Project by its ID.",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ALERT') && hasPermission(#projectId, '$REGISTRY_PROJECT_ALERT_R')")
	@GetMapping("/{id}")
	fun findAlertById(
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<AlertReaderDto>

	@Operation(
		summary = "Find Alert Communications",
		description = """
			List, paginated, the Communications posted on this Alert (its follow-up thread),
			optionally filtered by free-text search, visibility and a date/time range.
		""",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ALERT') && hasPermission(#projectId, '$REGISTRY_PROJECT_ALERT_COMMUNICATION_R')")
	@RateLimited(SEARCH, whenParamPresent = ["q"])
	@GetMapping("/{id}/communications")
	fun findAlertCommunications(
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@ParameterObject @Valid page: PageQueryDto,
		@RequestParam(name = "q", required = false) query: String?,
		@RequestParam(name = "visible", required = false) isVisible: Boolean?,
		@ParameterObject dateTimeRange: DateTimeRangeQueryDto,
	): Mono<PageReaderDto<CommunicationReaderDto>>

	@Operation(
		summary = "Find ongoing Alerts",
		description = """
			Dashboard widget: the most recent open (IN_PROGRESS) Alerts, each returned with its 3 most recent Communications
			for a contextualized preview. Results are capped at "limit" rows (default $DEFAULT_DASHBOARD_LIMIT, max $MAX_DASHBOARD_LIMIT).
		""",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ALERT') && hasPermission(#projectId, '$REGISTRY_PROJECT_ALERT_R')")
	@GetMapping("/ongoing")
	fun findOngoingAlerts(
		@PathVariable projectId: UUID,
		@RequestParam(defaultValue = DEFAULT_DASHBOARD_LIMIT)
		@Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
			MAX_DASHBOARD_LIMIT,
			message = PAGE_SIZE_EXCEEDS_MAX_PAGE_SIZE
		)
		limit: Int,
	): Flux<OngoingAlertReaderDto>

	@Operation(
		summary = "Create Alert",
		description = """
			Raise a new Alert on the Project, optionally linked to the Movement that triggered it (`movementId`).
			The Alert is created with an IN_PROGRESS status.
		""",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ALERT') && hasPermission(#projectId, '$REGISTRY_PROJECT_ALERT_C')")
	@RateLimited(SENSITIVE)
	@PostMapping
	fun createAlert(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@RequestBody @Valid alert: AlertCreationWriterDto,
	): Mono<ResponseEntity<AlertReaderDto>>

	@Operation(
		summary = "Update Alert",
		description = "Update an Alert's title, date/time and status in a single call.",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ALERT') && hasPermission(#projectId, '$REGISTRY_PROJECT_ALERT_U')")
	@RateLimited(SENSITIVE)
	@PatchMapping("/{id}")
	fun updateAlertById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid alert: AlertWriterDto,
	): Mono<AlertReaderDto>

	@Operation(
		summary = "Update Alert status",
		description = """
			Move the Alert to a new status only: IN_PROGRESS (still open), RESOLVED (closed, situation handled)
			or CANCELED (closed, raised in error).
		""",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ALERT') && hasPermission(#projectId, '$REGISTRY_PROJECT_ALERT_U')")
	@RateLimited(SENSITIVE)
	@PostMapping("/{id}/status")
	fun updateAlertStatusById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid status: AlertStatusWriterDto,
	): Mono<AlertReaderDto>

	@Operation(
		summary = "Disable Alert",
		description = "Soft-delete the Alert: it is kept (with its Communications) but hidden from the Project going forward.",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ALERT') && hasPermission(#projectId, '$REGISTRY_PROJECT_ALERT_U')")
	@RateLimited(SENSITIVE)
	@PostMapping("/{id}/disable")
	fun disableAlertById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<AlertReaderDto>

	@Operation(
		summary = "Enable Alert",
		description = "Reverse a disable: the Alert becomes visible in the Project again.",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ALERT') && hasPermission(#projectId, '$REGISTRY_PROJECT_ALERT_D')")
	@RateLimited(SENSITIVE)
	@PostMapping("/{id}/enable")
	fun enableAlertById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<AlertReaderDto>

	@Operation(
		summary = "Delete Alert",
		description = """
			Permanently delete the Alert and all its data, including its Communications. This cannot be undone;
			prefer disabling the Alert if it may be needed again.
		""",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ALERT') && hasPermission(#projectId, '$REGISTRY_PROJECT_ALERT_D')")
	@RateLimited(SENSITIVE)
	@ResponseStatus(NO_CONTENT)
	@DeleteMapping("/{id}")
	fun deleteAlertById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<Unit>
}
