package fr.laucoin.registry.backend.infrastructure.internal.web.controller

import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_MOVEMENT_C
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_MOVEMENT_D
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_MOVEMENT_METADATA_R
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_MOVEMENT_R
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_MOVEMENT_U
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementParticipantsAndGroupsModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.MovementWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
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
import reactor.core.publisher.Mono

@Tag(name = "Movements management", description = "API for Movements-related operations")
@RequestMapping("/api/events/{eventId}/movements")
interface IMovementController {
    @Operation(
        summary = "Find Movements",
        description = "Find or get paginated Movements"
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_MOVEMENT_R')")
    @GetMapping
    fun findMovements(
        @PathVariable eventId: UUID,
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "DESC") order: Direction,
        @RequestParam(defaultValue = "true") onlyVisible: Boolean,
        @RequestParam(required = false) searched: String?,
        @RequestParam(required = false) type: MovementTypeEnum?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DATE_TIME) startDateTime: ZonedDateTime?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DATE_TIME) endDateTime: ZonedDateTime?,
    ): Mono<PageDto<MovementReaderDto>>

    @Operation(
        summary = "Find Movement",
        description = "Find Movement by ID"
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_MOVEMENT_R')")
    @GetMapping("/{id}")
    fun findMovementById(@PathVariable eventId: UUID, @PathVariable id: UUID): Mono<MovementReaderDto>

    @Operation(
        summary = "Search Participants and/or Groups",
        description = "Search Participants and/or Groups to add in a Movement"
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_MOVEMENT_METADATA_R')")
    @GetMapping("/search/participants-and-groups")
    fun searchParticipantsAndGroups(
        @PathVariable eventId: UUID,
        @RequestParam searched: String?
    ): Mono<MovementParticipantsAndGroupsModel>

    @Operation(
        summary = "Create Movement",
        description = "Create Movement and related Movement Content"
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_MOVEMENT_C')")
    @PostMapping
    fun createMovement(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @PathVariable eventId: UUID,
        @RequestBody @Valid movement: MovementWriterDto,
    ): Mono<MovementModel>

    @Operation(
        summary = "Update Movement",
        description = "Update Movement"
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_MOVEMENT_U')")
    @PatchMapping("/{id}")
    fun updateMovementById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
        @RequestBody @Valid movement: MovementWriterDto,
    ): Mono<MovementModel>

    @Operation(
        summary = "Disable Movement",
        description = "Disable Movement, it will not visible anymore in the Event"
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_MOVEMENT_U')")
    @PatchMapping("/{id}/disable")
    fun disableMovementById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
    ): Mono<MovementModel>

    @Operation(
        summary = "Enable Movement",
        description = "Enable Movement, obviously it will be visible again in the Event"
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_MOVEMENT_U')")
    @PatchMapping("/{id}/enable")
    fun enableMovementById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
    ): Mono<MovementModel>

    @Operation(
        summary = "Delete Movement",
        description = "Delete all Movement data."
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_MOVEMENT_D')")
    @DeleteMapping("/{id}")
    fun deleteMovementById(@PathVariable eventId: UUID, @PathVariable id: UUID): Mono<Void>
}
