package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.annotation.RateLimited
import fr.laucoin.registry.backend.domain.constant.ApiConst.API_V2
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_VEHICLE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_HISTORY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_U
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SEARCH
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SENSITIVE
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.PageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.SortedPageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PageReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.VehicleWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.UUID

@Tag(name = "Vehicles management", description = "API for Vehicles-related operations")
@RequestMapping("$API_V2/projects/{projectId}/vehicles")
interface IVehicleV2Controller {
	@Operation(
		summary = "Find Vehicles",
		description = "Find or get paginated Vehicles",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '$REGISTRY_PROJECT_VEHICLE_R')")
	@RateLimited(SEARCH, whenParamPresent = ["q"])
	@GetMapping
	fun findVehicles(
		@PathVariable projectId: UUID,
		@ParameterObject @Valid page: SortedPageQueryDto,
		@RequestParam(required = false) q: String?,
		@RequestParam(required = false) visible: Boolean?,
		@RequestParam(required = false) status: PresenceStatusEnum?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) dateTime: ZonedDateTime?,
	): Mono<PageReaderDto<VehicleReaderDto>>

	@Operation(
		summary = "Find Vehicle",
		description = "Find Vehicle by ID",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '$REGISTRY_PROJECT_VEHICLE_R')")
	@GetMapping("/{id}")
	fun findVehicleById(
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<VehicleReaderDto>

	@Operation(
		summary = "Find Vehicle Movements",
		description = "Find or get paginated vehicle Movements",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '$REGISTRY_PROJECT_VEHICLE_HISTORY_R')")
	@GetMapping("/{id}/movements")
	fun findVehicleMovements(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@ParameterObject @Valid page: PageQueryDto,
		@RequestParam(required = false) visible: Boolean?,
		@Parameter(description = "\"true\" value will be considered only if the project has REGISTRY_PROJECT_OPTION_ACTIVITY.")
		@RequestParam(required = false) linkedToActivity: Boolean?,
		@RequestParam(required = false) type: MovementTypeEnum?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) startDateTime: ZonedDateTime?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE_TIME) endDateTime: ZonedDateTime?,
	): Mono<PageReaderDto<MovementReaderDto>>

	@Operation(
		summary = "Create Vehicle",
		description = "Create Vehicle linked to the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '$REGISTRY_PROJECT_VEHICLE_C')")
	@RateLimited(SENSITIVE)
	@PostMapping
	fun createVehicle(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@RequestBody @Valid vehicle: VehicleWriterDto,
	): Mono<ResponseEntity<VehicleReaderDto>>

	@Operation(
		summary = "Update Vehicle",
		description = "Update Vehicle",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '$REGISTRY_PROJECT_VEHICLE_U')")
	@RateLimited(SENSITIVE)
	@PatchMapping("/{id}")
	fun updateVehicleById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid vehicle: VehicleWriterDto,
	): Mono<VehicleReaderDto>

	@Operation(
		summary = "Disable Vehicle",
		description = "Disable Vehicle, it will not visible anymore in the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '$REGISTRY_PROJECT_VEHICLE_U')")
	@RateLimited(SENSITIVE)
	@PostMapping("/{id}/disable")
	fun disableVehicleById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<VehicleReaderDto>

	@Operation(
		summary = "Enable Vehicle",
		description = "Enable Vehicle, obviously it will be visible again in the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '$REGISTRY_PROJECT_VEHICLE_U')")
	@RateLimited(SENSITIVE)
	@PostMapping("/{id}/enable")
	fun enableVehicleById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<VehicleReaderDto>

	@Operation(
		summary = "Delete Vehicle",
		description = "Delete all Vehicle data.",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '$REGISTRY_PROJECT_VEHICLE_D')")
	@RateLimited(SENSITIVE)
	@ResponseStatus(NO_CONTENT)
	@DeleteMapping("/{id}")
	fun deleteVehicleById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<Unit>
}
