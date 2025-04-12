package fr.laucoin.registry.backend.infrastructure.internal.web.controller

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_D
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_U
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_EVENT_C
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_EVENT_METADATA_R
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.EventOptionsReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.EventReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.EventWriterDto
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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Tag(name = "Events management", description = "API for Events-related operations")
@RequestMapping("/api/events")
interface IEventController {
    @Operation(
        summary = "Find Events",
        description = "Find or get paginated Events",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @GetMapping
    fun findEvents(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) pageNumber: Int,
        @RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
            200,
            message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
        ) pageSize: Int,
        @RequestParam(required = false) textSearched: String?,
        @RequestParam(required = false) visibilitySearched: Boolean?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DATE_TIME) dateTimeSearched: ZonedDateTime?,
    ): Mono<PageModel<EventReaderDto>>

    @Operation(
        summary = "Find Event",
        description = "Find Event by ID",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasAuthority('${UserPermissionConst.REGISTRY_EVENT_R}') || hasPermission(#id, '${EventPermissionConst.REGISTRY_EVENT_R}')")
    @GetMapping("/{id}")
    fun findEventById(@RequestHeader(ACCEPT_LANGUAGE) locale: Locale, @PathVariable id: UUID): Mono<EventReaderDto>

    @Operation(
        summary = "Get available Options",
        description = "Get all the Options you are allowed to enable",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasAuthority('$REGISTRY_EVENT_METADATA_R')")
    @GetMapping("/options")
    fun getAvailableEventOptions(@RequestHeader(ACCEPT_LANGUAGE) locale: Locale): Flux<EventOptionsReaderDto>

    @Operation(
        summary = "Create Event",
        description = "Create Event and Event Profile administration for the Current User",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasAuthority('$REGISTRY_EVENT_C')")
    @PostMapping
    fun createEvent(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @RequestBody @Valid event: EventWriterDto,
    ): Mono<EventReaderDto>

    @Operation(
        summary = "Update Event",
        description = "Update Event",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#id, '$REGISTRY_EVENT_U')")
    @PatchMapping("/{id}")
    fun updateEventById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable id: UUID,
        @RequestBody @Valid event: EventWriterDto
    ): Mono<EventReaderDto>

    @Operation(
        summary = "Disable Event",
        description = "Disable Event access, obviously the related profile is no accessible anymore.",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#id, '$REGISTRY_EVENT_U')")
    @PatchMapping("/{id}/disable")
    fun disableEventById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable id: UUID,
    ): Mono<EventReaderDto>

    @Operation(
        summary = "Enable Event",
        description = "Enable Event, obviously the profiles concerned are accessible again.",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#id, '$REGISTRY_EVENT_U')")
    @PatchMapping("/{id}/enable")
    fun enableEventById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable id: UUID,
    ): Mono<EventReaderDto>

    @Operation(
        summary = "Delete Event",
        description = "Delete all Event data.",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#id, '$REGISTRY_EVENT_D')")
    @DeleteMapping("/{id}")
    fun deleteEventById(@PathVariable id: UUID): Mono<Void>
}
