package fr.laucoin.registry.backend.infrastructure.internal.web.controller

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.EventDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Mono

@Tag(name = "Events management", description = "API for Events-related operations")
@RequestMapping("/api/events")
interface IEventController {
    @Operation(
        summary = "Find Events",
        description = "Find or get paginated Events"
    )
    @GetMapping
    fun findEvents(
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "ASC") order: Direction,
        @RequestParam(defaultValue = "true") onlyVisible: Boolean,
        @RequestParam(required = false) searched: String?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DATE_TIME) startDateTime: ZonedDateTime?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DATE_TIME) endDateTime: ZonedDateTime?,
    ): Mono<PageModel<EventModel>>

    @Operation(
        summary = "Find Event",
        description = "Find Event by ID"
    )
    @PreAuthorize("hasAuthority('REGISTRY_EVENT_R') || hasPermission(#id, 'REGISTRY_EVENT_R')")
    @GetMapping("/{id}")
    fun findEventById(@PathVariable id: UUID): Mono<EventModel>

    @Operation(
        summary = "Create Event",
        description = "Create Event and Event Profile administration for the Current User"
    )
    @PreAuthorize("hasAuthority('REGISTRY_EVENT_C')")
    @PostMapping
    fun createEvent(@RequestBody @Valid event: EventDto): Mono<EventModel>

    @Operation(
        summary = "Update Event",
        description = "Update Event"
    )
    @PreAuthorize("hasPermission(#id, 'REGISTRY_EVENT_U')")
    @PatchMapping("/{id}")
    fun updateEventById(@PathVariable id: UUID, @RequestBody @Valid event: EventDto): Mono<EventModel>

    @Operation(
        summary = "Disable Event",
        description = "Disable Event access, obviously the related profile is no accessible anymore."
    )
    @PreAuthorize("hasPermission(#id, 'REGISTRY_EVENT_U')")
    @PatchMapping("/{id}/disable")
    fun disableEventById(@PathVariable id: UUID): Mono<EventModel>

    @Operation(
        summary = "Enable Event",
        description = "Enable Event, obviously the profiles concerned are accessible again."
    )
    @PreAuthorize("hasPermission(#id, 'REGISTRY_EVENT_U')")
    @PatchMapping("/{id}/enable")
    fun enableEventById(@PathVariable id: UUID): Mono<EventModel>

    @Operation(
        summary = "Delete Event",
        description = "Delete all Event data."
    )
    @PreAuthorize("hasPermission(#id, 'REGISTRY_EVENT_D')")
    @DeleteMapping("/{id}")
    fun deleteEventById(@PathVariable id: UUID): Mono<Void>
}
