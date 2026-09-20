package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.annotation.RateLimited
import fr.laucoin.registry.backend.domain.constant.ApiConst.API_V2
import fr.laucoin.registry.backend.domain.constant.ApiConst.DEFAULT_DASHBOARD_LIMIT
import fr.laucoin.registry.backend.domain.constant.ApiConst.MAX_DASHBOARD_LIMIT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_HISTORY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_U
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SEARCH
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SENSITIVE
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.PageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.SortedPageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ActivityReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.OngoingActivityOutingReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PageReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ActivityWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
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

@Tag(name = "Activities management", description = "API for Activities-related operations")
@RequestMapping("$API_V2/projects/{projectId}/activities")
interface IActivityV2Controller {
	@Operation(
		summary = "Find Activities",
		description = "Find or get paginated Activities",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_ACTIVITY_R')")
	@RateLimited(SEARCH, whenParamPresent = ["q"])
	@GetMapping
	fun findActivities(
		@PathVariable projectId: UUID,
		@ParameterObject @Valid page: SortedPageQueryDto,
		@RequestParam(required = false) q: String?,
		@RequestParam(required = false) visible: Boolean?,
		@RequestParam(required = false) available: Boolean?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) dateTime: ZonedDateTime?,
	): Mono<PageReaderDto<ActivityReaderDto>>

	@Operation(
		summary = "Find Activity",
		description = "Find Activity by ID",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_ACTIVITY_R')")
	@GetMapping("/{id}")
	fun findActivityById(
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
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@ParameterObject @Valid page: PageQueryDto,
		@RequestParam(required = false) visible: Boolean?,
		@RequestParam(required = false) type: MovementTypeEnum?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) startDateTime: ZonedDateTime?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) endDateTime: ZonedDateTime?,
	): Mono<PageReaderDto<MovementReaderDto>>

	@Operation(
		summary = "Find ongoing Activity outings",
		description = "Activities whose last Movement is an outing (OUT), each with their 3 most recent Communications for a contextualized preview; capped at \"limit\" rows",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_MOVEMENT_R')")
	@GetMapping("/ongoing")
	fun findOngoingActivityOutings(
		@PathVariable projectId: UUID,
		@RequestParam(defaultValue = DEFAULT_DASHBOARD_LIMIT)
		@Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(MAX_DASHBOARD_LIMIT, message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE)
		limit: Int,
	): Flux<OngoingActivityOutingReaderDto>

	@Operation(
		summary = "Create Activity",
		description = "Create Activity linked to the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_ACTIVITY_C')")
	@RateLimited(SENSITIVE)
	@PostMapping
	fun createActivity(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@RequestBody @Valid activity: ActivityWriterDto,
	): Mono<ResponseEntity<ActivityReaderDto>>

	@Operation(
		summary = "Update Activity",
		description = "Update Activity",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_ACTIVITY_U')")
	@RateLimited(SENSITIVE)
	@PatchMapping("/{id}")
	fun updateActivityById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid activity: ActivityWriterDto,
	): Mono<ActivityReaderDto>

	@Operation(
		summary = "Disable Activity",
		description = "Disable Activity, it will not visible anymore in the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_ACTIVITY_U')")
	@RateLimited(SENSITIVE)
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
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_ACTIVITY_U')")
	@RateLimited(SENSITIVE)
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
	@RateLimited(SENSITIVE)
	@ResponseStatus(NO_CONTENT)
	@DeleteMapping("/{id}")
	fun deleteActivityById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<Unit>
}
