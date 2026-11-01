package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_ALLOWED_PARTICIPANTS_TOO_LOW
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_UPPER_THAN_MAX_PAGE
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_DEFAULT
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_MIN
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_SIZE_DEFAULT
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_SIZE_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_SIZE_MIN
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_FORMAT_INVALID
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_HISTORY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_U
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ACTIVITY
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.ACTIVITY_DURATION_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.DURATION_PATTERN
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SEARCH_QUERY_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SORT_MAX
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ActivityReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ActivityWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
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
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat REST des endpoints de l'activité sous /api/v2.
 * Périmètre : Porte les routes, les règles d'autorisation, les bornes de validation et les métadonnées OpenAPI.
 * Limites : Ne déclare aucun comportement ; l'implémentation du contrôleur délègue aux services.
 */
@Tag(name = "Activities management", description = "API for Activities-related operations")
@RequestMapping("/api/v2/projects/{projectId}/activities")
interface IActivityV2Controller {
	@Operation(
		summary = "Find Activities",
		description = "Find or get paginated Activities",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_ACTIVITY_R')")
	@GetMapping
	fun findActivities(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@RequestParam(defaultValue = PAGE_DEFAULT) @Valid @Min(PAGE_MIN, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) @Max(PAGE_MAX, message = PAGE_NUMBER_IS_UPPER_THAN_MAX_PAGE) page: Long,
		@RequestParam(defaultValue = PAGE_SIZE_DEFAULT) @Valid @Min(PAGE_SIZE_MIN, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(PAGE_SIZE_MAX, message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE) size: Int,
		@Parameter(description = "Text matched against the activity name and description")
		@RequestParam(value = "q", required = false) @Valid @Size(max = SEARCH_QUERY_MAX, message = PARAMETER_TOO_LONG) query: String?,
		@Parameter(description = "Visibility filter: true keeps visible elements only, false keeps hidden ones only, omitted keeps both")
		@RequestParam(required = false) isVisible: Boolean?,
		@Parameter(description = "Availability filter at the current time: true keeps available elements only, false keeps unavailable ones only, omitted keeps both")
		@RequestParam(required = false) isAvailable: Boolean?,
		@Parameter(description = "Instant that must fall inside the validity window")
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) at: ZonedDateTime?,
		@Parameter(description = "Minimum activity duration (inclusive), ISO 8601 such as PT1H30M. Activities without duration are excluded")
		@RequestParam(required = false)
		@Valid
		@Size(max = ACTIVITY_DURATION_MAX, message = PARAMETER_TOO_LONG)
		@Pattern(regexp = DURATION_PATTERN, message = PARAMETER_FORMAT_INVALID) minDuration: String?,
		@Parameter(description = "Maximum activity duration (inclusive), ISO 8601 such as PT1H30M. Activities without duration are excluded")
		@RequestParam(required = false)
		@Valid
		@Size(max = ACTIVITY_DURATION_MAX, message = PARAMETER_TOO_LONG)
		@Pattern(regexp = DURATION_PATTERN, message = PARAMETER_FORMAT_INVALID) maxDuration: String?,
		@Parameter(description = "Number of participants the activity must be able to host, within its allowed participants range")
		@RequestParam(required = false) @Valid @Min(1, message = ACTIVITY_ALLOWED_PARTICIPANTS_TOO_LOW) participants: Int?,
		@Parameter(description = "Sort field, prefixed with - for descending order. Supported: name, description, duration, minAllowedParticipants, maxAllowedParticipants, startAvailability, endAvailability")
		@RequestParam(required = false) @Valid @Size(max = SORT_MAX, message = PARAMETER_TOO_LONG) sort: String?,
	): Mono<PageModel<ActivityReaderDto>>

	@Operation(
		summary = "Find Activity",
		description = "Find Activity by ID",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_ACTIVITY_R')")
	@GetMapping("/{id}")
	fun findActivityById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<ActivityReaderDto>

	@Operation(
		summary = "Find Activity Movements",
		description = "Find or get paginated activity Movements",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_ACTIVITY_HISTORY_R')")
	@GetMapping("/{id}/movements")
	fun findActivityMovements(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestParam(defaultValue = PAGE_DEFAULT) @Valid @Min(PAGE_MIN, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) @Max(PAGE_MAX, message = PAGE_NUMBER_IS_UPPER_THAN_MAX_PAGE) page: Long,
		@RequestParam(defaultValue = PAGE_SIZE_DEFAULT) @Valid @Min(PAGE_SIZE_MIN, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(PAGE_SIZE_MAX, message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE) size: Int,
		@Parameter(description = "Visibility filter: true keeps visible elements only, false keeps hidden ones only, omitted keeps both")
		@RequestParam(required = false) isVisible: Boolean?,
		@RequestParam(required = false) type: MovementTypeEnum?,
		@Parameter(description = "Start (inclusive) of the period that must contain the element date-time")
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) from: ZonedDateTime?,
		@Parameter(description = "End (inclusive) of the period that must contain the element date-time")
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) to: ZonedDateTime?,
		@Parameter(description = "Sort field, prefixed with - for descending order. Supported: dateTime, type")
		@RequestParam(required = false) @Valid @Size(max = SORT_MAX, message = PARAMETER_TOO_LONG) sort: String?,
	): Mono<PageModel<MovementReaderDto>>

	@Operation(
		summary = "Create Activity",
		description = "Create Activity linked to the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_ACTIVITY_C')")
	@PostMapping
	fun createActivity(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@RequestBody @Valid activity: ActivityWriterDto,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<ActivityReaderDto>>

	@Operation(
		summary = "Update Activity",
		description = "Update Activity",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_ACTIVITY_U')")
	@PatchMapping("/{id}")
	fun updateActivityById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid activity: ActivityWriterDto,
		exchange: ServerWebExchange,
	): Mono<ActivityReaderDto>

	@Operation(
		summary = "Disable Activity",
		description = "Disable Activity, it will not visible anymore in the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_ACTIVITY_U')")
	@PostMapping("/{id}/disable")
	fun disableActivityById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<ActivityReaderDto>

	@Operation(
		summary = "Enable Activity",
		description = "Enable Activity, obviously it will be visible again in the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_ACTIVITY_D')")
	@PostMapping("/{id}/enable")
	fun enableActivityById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<ActivityReaderDto>

	@Operation(
		summary = "Delete Activity",
		description = "Delete all Activity data.",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_ACTIVITY_D')")
	@DeleteMapping("/{id}")
	@ResponseStatus(NO_CONTENT)
	fun deleteActivityById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<Unit>
}
