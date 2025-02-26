package fr.laucoin.registry.backend.infrastructure.internal.web.controller

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_ACTIVITY_C
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_ACTIVITY_D
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_ACTIVITY_HISTORY_R
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_ACTIVITY_R
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_ACTIVITY_U
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_OPTION_ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ActivityReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.ActivityWriterDto
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

@Tag(name = "Activities management", description = "API for Activities-related operations")
@RequestMapping("/api/events/{eventId}/activities")
interface IActivityController {
    @Operation(
        summary = "Find Activities",
        description = "Find or get paginated Activities",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_OPTION_ACTIVITY') && hasPermission(#eventId, '$REGISTRY_EVENT_ACTIVITY_R')")
    @GetMapping
    fun findActivities(
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) pageNumber: Int,
        @RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
            200,
            message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
        ) pageSize: Int,
        @RequestParam(required = false) textSearched: String?,
        @RequestParam(required = false) visibilitySearched: Boolean?,
        @RequestParam(required = false) availabilitySearched: Boolean?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DATE_TIME) dateTimeSearched: ZonedDateTime?,
    ): Mono<PageModel<ActivityReaderDto>>

    @Operation(
        summary = "Find Activity",
        description = "Find Activity by ID",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_OPTION_ACTIVITY') && hasPermission(#eventId, '$REGISTRY_EVENT_ACTIVITY_R')")
    @GetMapping("/{id}")
    fun findActivityById(
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
    ): Mono<ActivityReaderDto>

    @Operation(
        summary = "Find Activity Movements",
        description = "Find or get paginated activity Movements",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_OPTION_ACTIVITY') && hasPermission(#eventId, '$REGISTRY_EVENT_ACTIVITY_HISTORY_R')")
    @GetMapping("/{id}/movements")
    fun findActivityMovements(
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
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
        summary = "Create Activity",
        description = "Create Activity linked to the Event",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_OPTION_ACTIVITY') && hasPermission(#eventId, '$REGISTRY_EVENT_ACTIVITY_C')")
    @PostMapping
    fun createActivity(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @RequestBody @Valid activity: ActivityWriterDto,
    ): Mono<ActivityReaderDto>

    @Operation(
        summary = "Update Activity",
        description = "Update Activity",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_OPTION_ACTIVITY') && hasPermission(#eventId, '$REGISTRY_EVENT_ACTIVITY_U')")
    @PatchMapping("/{id}")
    fun updateActivityById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
        @RequestBody @Valid activity: ActivityWriterDto,
    ): Mono<ActivityReaderDto>

    @Operation(
        summary = "Disable Activity",
        description = "Disable Activity, it will not visible anymore in the Event",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_OPTION_ACTIVITY') && hasPermission(#eventId, '$REGISTRY_EVENT_ACTIVITY_U')")
    @PatchMapping("/{id}/disable")
    fun disableActivityById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
    ): Mono<ActivityReaderDto>

    @Operation(
        summary = "Enable Activity",
        description = "Enable Activity, obviously it will be visible again in the Event",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_OPTION_ACTIVITY') && hasPermission(#eventId, '$REGISTRY_EVENT_ACTIVITY_U')")
    @PatchMapping("/{id}/enable")
    fun enableActivityById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
    ): Mono<ActivityReaderDto>

    @Operation(
        summary = "Delete Activity",
        description = "Delete all Activity data.",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_OPTION_ACTIVITY') && hasPermission(#eventId, '$REGISTRY_EVENT_ACTIVITY_D')")
    @DeleteMapping("/{id}")
    fun deleteActivityById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
    ): Mono<Void>
}
