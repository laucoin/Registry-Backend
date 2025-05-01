package fr.laucoin.registry.backend.infrastructure.internal.web.controller

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_VEHICLE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_HISTORY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_U
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.UsableElementStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.VehicleWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Mono

@Tag(name = "Vehicles management", description = "API for Vehicles-related operations")
@RequestMapping("/api/projects/{projectId}/vehicles")
interface IVehicleController {
    @Operation(
        summary = "Find Vehicles",
        description = "Find or get paginated Vehicles",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '$REGISTRY_PROJECT_VEHICLE_R')")
    @GetMapping
    fun findVehicles(
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable projectId: UUID,
        @RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) pageNumber: Int,
        @RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
            200,
            message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
        ) pageSize: Int,
        @RequestParam(required = false) textSearched: String?,
        @RequestParam(required = false) visibilitySearched: Boolean?,
        @RequestParam(required = false) statusSearched: UsableElementStatusEnum?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DATE_TIME) dateTimeSearched: ZonedDateTime?,
    ): Mono<PageModel<VehicleReaderDto>>

    @Operation(
        summary = "Find Vehicle",
        description = "Find Vehicle by ID",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '$REGISTRY_PROJECT_VEHICLE_R')")
    @GetMapping("/{id}")
    fun findVehicleById(
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable projectId: UUID,
        @PathVariable id: UUID,
    ): Mono<VehicleReaderDto>

    @Operation(
        summary = "Find Vehicle Movements",
        description = "Find or get paginated vehicle Movements",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '$REGISTRY_PROJECT_VEHICLE_HISTORY_R')")
    @GetMapping("/{id}/movements")
    fun findVehicleMovements(
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable projectId: UUID,
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) pageNumber: Int,
        @RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
            200,
            message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
        ) pageSize: Int,
        @RequestParam(required = false) visibilitySearched: Boolean?,
        @RequestParam(required = false) typeSearched: MovementTypeEnum?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DATE_TIME) startDateTimeSearched: ZonedDateTime?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DATE_TIME) endDateTimeSearched: ZonedDateTime?,
    ): Mono<PageModel<MovementReaderDto>>

    @Operation(
        summary = "Create Vehicle",
        description = "Create Vehicle linked to the Project",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '$REGISTRY_PROJECT_VEHICLE_C')")
    @PostMapping
    fun createVehicle(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable projectId: UUID,
        @RequestBody @Valid vehicle: VehicleWriterDto,
    ): Mono<VehicleReaderDto>

    @Operation(
        summary = "Update Vehicle",
        description = "Update Vehicle",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '$REGISTRY_PROJECT_VEHICLE_U')")
    @PatchMapping("/{id}")
    fun updateVehicleById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable projectId: UUID,
        @PathVariable id: UUID,
        @RequestBody @Valid vehicle: VehicleWriterDto,
    ): Mono<VehicleReaderDto>

    @Operation(
        summary = "Disable Vehicle",
        description = "Disable Vehicle, it will not visible anymore in the Project",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '$REGISTRY_PROJECT_VEHICLE_U')")
    @PatchMapping("/{id}/disable")
    fun disableVehicleById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable projectId: UUID,
        @PathVariable id: UUID,
    ): Mono<VehicleReaderDto>

    @Operation(
        summary = "Enable Vehicle",
        description = "Enable Vehicle, obviously it will be visible again in the Project",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '$REGISTRY_PROJECT_VEHICLE_U')")
    @PatchMapping("/{id}/enable")
    fun enableVehicleById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable projectId: UUID,
        @PathVariable id: UUID,
    ): Mono<VehicleReaderDto>

    @Operation(
        summary = "Delete Vehicle",
        description = "Delete all Vehicle data.",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_VEHICLE') && hasPermission(#projectId, '$REGISTRY_PROJECT_VEHICLE_D')")
    @DeleteMapping("/{id}")
    fun deleteVehicleById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @PathVariable projectId: UUID,
        @PathVariable id: UUID,
    ): Mono<Void>
}
