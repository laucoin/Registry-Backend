package fr.laucoin.registry.backend.infrastructure.internal.web.controller

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_MOVEMENT_C
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_MOVEMENT_D
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_MOVEMENT_METADATA_R
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_MOVEMENT_R
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_MOVEMENT_U
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementParticipantsAndGroupsReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReaderDto.MovementContentReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReasonsReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.GuestMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.ParticipantMovementWriterDto
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

@Tag(name = "Movements management", description = "API for Movements-related operations")
@RequestMapping("/api/events/{eventId}/movements")
interface IMovementController {
    @Operation(
        summary = "Find Movements",
        description = "Find or get paginated Movements without content",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_MOVEMENT_R')")
    @GetMapping
    fun findMovements(
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
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
        summary = "Find Movements contents",
        description = "Find or get content of given Movements IDs",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_MOVEMENT_R')")
    @GetMapping("/contents")
    fun findMovementsContents(
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @RequestParam(required = true) movementIds: List<UUID>,
    ): Flux<Pair<UUID, List<MovementContentReaderDto>>>

    @Operation(
        summary = "Find Movement",
        description = "Find Movement by ID with content",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_MOVEMENT_R')")
    @GetMapping("/{id}")
    fun findMovementById(
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
    ): Mono<MovementReaderDto>

    @Operation(
        summary = "Search Reasons (and Activity as reason)",
        description = "Search Reasons (and Activity as reason) to add in a Movement",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_MOVEMENT_METADATA_R')")
    @GetMapping("/search/reasons")
    fun searchReasonsAndActivities(
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @RequestParam(required = true) typeSearched: MovementTypeEnum,
        @RequestParam(required = true) contentTypeSearched: ParticipantTypeEnum,
        @RequestParam textSearched: String?,
    ): Flux<MovementReasonsReaderDto>

    @Operation(
        summary = "Search Participants and/or Groups",
        description = "Search Participants and/or Groups to add in a Movement",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_MOVEMENT_METADATA_R')")
    @GetMapping("/search/participants-and-groups")
    fun searchParticipantsAndGroups(
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @RequestParam(required = true) contentTypeSearched: ParticipantTypeEnum,
        @RequestParam textSearched: String?,
    ): Mono<MovementParticipantsAndGroupsReaderDto>

    @Operation(
        summary = "Search Vehicles",
        description = "Search Vehicles to add in a Movement",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_MOVEMENT_METADATA_R')")
    @GetMapping("/search/vehicles")
    fun searchVehicles(
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @RequestParam textSearched: String?
    ): Flux<VehicleReaderDto>

    @Operation(
        summary = "Create Movement",
        description = "Create Movement and related Content",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_MOVEMENT_C')")
    @PostMapping
    fun createMovement(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @RequestBody @Valid movement: ParticipantMovementWriterDto,
    ): Mono<MovementReaderDto>

    @Operation(
        summary = "Update Movement",
        description = "Update Movement and related Content",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_MOVEMENT_U')")
    @PatchMapping("/{id}")
    fun updateMovementById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
        @RequestBody @Valid movement: ParticipantMovementWriterDto,
    ): Mono<MovementReaderDto>

    @Operation(
        summary = "Create Guest Movement",
        description = "Create Movement and related Guest Content",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_MOVEMENT_C')")
    @PostMapping("/guests")
    fun createGuestsMovement(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @RequestBody @Valid movement: GuestMovementWriterDto,
    ): Mono<MovementReaderDto>

    @Operation(
        summary = "Update Guest Movement",
        description = "Update Movement and related Guest Content",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_MOVEMENT_U')")
    @PatchMapping("/guests/{id}")
    fun updateGuestsMovementById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
        @RequestBody @Valid movement: GuestMovementWriterDto,
    ): Mono<MovementReaderDto>

    @Operation(
        summary = "Disable Movement",
        description = "Disable Movement, it will not visible anymore in the Event",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_MOVEMENT_U')")
    @PatchMapping("/{id}/disable")
    fun disableMovementById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
    ): Mono<MovementReaderDto>

    @Operation(
        summary = "Enable Movement",
        description = "Enable Movement, obviously it will be visible again in the Event",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_MOVEMENT_U')")
    @PatchMapping("/{id}/enable")
    fun enableMovementById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
    ): Mono<MovementReaderDto>

    @Operation(
        summary = "Delete Movement",
        description = "Delete all Movement data.",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_MOVEMENT_D')")
    @DeleteMapping("/{id}")
    fun deleteMovementById(@PathVariable eventId: UUID, @PathVariable id: UUID): Mono<Void>
}
