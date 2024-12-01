package fr.laucoin.registry.backend.infrastructure.internal.web.controller

import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.ParticipantDto
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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Tag(name = "Participants management", description = "API for Participants-related operations")
@RequestMapping("/api/events/{eventId}/participants")
interface IParticipantController {
    @Operation(
        summary = "Find Participants",
        description = "Find or get paginated Participants"
    )
    @PreAuthorize("hasPermission(#eventId, 'REGISTRY_EVENT_PARTICIPANT_R')")
    @GetMapping
    fun findParticipants(
        @PathVariable eventId: UUID,
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "ASC") order: Direction,
        @RequestParam(defaultValue = "true") onlyVisible: Boolean,
        @RequestParam(defaultValue = "true") onlyPresent: Boolean,
        @RequestParam(required = false) searched: String?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DATE_TIME) startDateTime: ZonedDateTime?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DATE_TIME) endDateTime: ZonedDateTime?,
    ): Mono<PageModel<ParticipantModel>>

    @Operation(
        summary = "Find Participant",
        description = "Find Participant by ID"
    )
    @PreAuthorize("hasPermission(#eventId, 'REGISTRY_EVENT_PARTICIPANT_R')")
    @GetMapping("/{id}")
    fun findParticipantById(@PathVariable eventId: UUID, @PathVariable id: UUID): Mono<ParticipantModel>

    @Operation(
        summary = "Search Participant",
        description = "Search non blocked Participant first or last name"
    )
    @PreAuthorize("hasPermission(#eventId, 'REGISTRY_EVENT_PARTICIPANT_R')")
    @GetMapping("/search")
    fun searchParticipants(
        @PathVariable eventId: UUID,
        @RequestParam(defaultValue = "true") onlyPresent: Boolean,
        @RequestParam(required = false) searched: String?,
    ): Flux<ParticipantModel>

    @Operation(
        summary = "Create Participant",
        description = "Create Participant linked to the Event"
    )
    @PreAuthorize("hasPermission(#eventId, 'REGISTRY_EVENT_PARTICIPANT_C')")
    @PostMapping
    fun createParticipant(@PathVariable eventId: UUID, @RequestBody @Valid participant: ParticipantDto): Mono<ParticipantModel>

    @Operation(
        summary = "Update Participant",
        description = "Update Participant"
    )
    @PreAuthorize("hasPermission(#eventId, 'REGISTRY_EVENT_PARTICIPANT_U')")
    @PatchMapping("/{id}")
    fun updateParticipantById(
        @PathVariable eventId: UUID, @PathVariable id: UUID, @RequestBody @Valid participant: ParticipantDto,
    ): Mono<ParticipantModel>

    @Operation(
        summary = "Disable Participant",
        description = "Disable Participant, it will not visible anymore in the Event"
    )
    @PreAuthorize("hasPermission(#eventId, 'REGISTRY_EVENT_PARTICIPANT_U')")
    @PatchMapping("/{id}/disable")
    fun disableParticipantById(@PathVariable eventId: UUID, @PathVariable id: UUID): Mono<ParticipantModel>

    @Operation(
        summary = "Enable Participant",
        description = "Enable Participant, obviously it will be visible again in the Event"
    )
    @PreAuthorize("hasPermission(#eventId, 'REGISTRY_EVENT_PARTICIPANT_U')")
    @PatchMapping("/{id}/enable")
    fun enableParticipantById(@PathVariable eventId: UUID, @PathVariable id: UUID): Mono<ParticipantModel>

    @Operation(
        summary = "Delete Participant",
        description = "Delete all Participant data."
    )
    @PreAuthorize("hasPermission(#eventId, 'REGISTRY_EVENT_PARTICIPANT_D')")
    @DeleteMapping("/{id}")
    fun deleteParticipantById(@PathVariable eventId: UUID, @PathVariable id: UUID): Mono<Void>
}
