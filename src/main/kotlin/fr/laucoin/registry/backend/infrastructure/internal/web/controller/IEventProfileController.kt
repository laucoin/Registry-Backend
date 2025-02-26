package fr.laucoin.registry.backend.infrastructure.internal.web.controller

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_PROFILE_C
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_PROFILE_D
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_PROFILE_METADATA_R
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_PROFILE_R
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_PROFILE_U
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.CreatedEventProfilesReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.EventProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.EventProfileWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.EventProfilesWriterDto
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
import org.springframework.http.ResponseEntity
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

@Tag(name = "Event's Profiles management", description = "API for Event's Profiles-related operations")
@RequestMapping("/api/events/{eventId}/profiles")
interface IEventProfileController {
    @Operation(
        summary = "Find Event's Profiles",
        description = "Find or get paginated Event's Profiles",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_PROFILE_R')")
    @GetMapping
    fun findEventProfiles(
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) pageNumber: Int,
        @RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
            200,
            message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
        ) pageSize: Int,
        @RequestParam(required = false) textSearched: String?,
        @RequestParam(required = false) availabilitySearched: Boolean?,
        @RequestParam(required = false) statusSearched: ProfileStatusEnum?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DATE_TIME) dateTimeSearched: ZonedDateTime?,
    ): Mono<PageModel<EventProfileReaderDto>>

    @Operation(
        summary = "Find Event's Profile",
        description = "Find Event's Profile by ID",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_PROFILE_R')")
    @GetMapping("/{id}")
    fun findEventProfileById(
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
    ): Mono<EventProfileReaderDto>

    @Operation(
        summary = "Search Users",
        description = "Search Users to invite to an Event",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_PROFILE_METADATA_R')")
    @GetMapping("/search/users")
    fun searchUsers(
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @RequestParam textSearched: String?,
    ): Flux<PartialUserReaderDto>

    @Operation(
        summary = "Get assignable Roles",
        description = "Get all the roles you are allowed to assign",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_PROFILE_METADATA_R')")
    @GetMapping("/roles")
    fun getAssignableEventProfileRoles(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
    ): Flux<LabelDto>

    @Operation(
        summary = "Create Event's Profiles",
        description = "Create Event's Profiles (multiple Users)",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_PROFILE_C')")
    @PostMapping
    fun createEventProfiles(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @RequestBody @Valid profiles: EventProfilesWriterDto,
    ): Mono<ResponseEntity<CreatedEventProfilesReaderDto>>

    @Operation(
        summary = "Update Event's Profile",
        description = "Update Event's Profile",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_PROFILE_U')")
    @PatchMapping("/{id}")
    fun updateEventProfile(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
        @RequestBody @Valid profile: EventProfileWriterDto,
    ): Mono<EventProfileReaderDto>

    @Operation(
        summary = "Block Event's Profile",
        description = "Prevent a User from using it",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_PROFILE_U')")
    @PatchMapping("/{id}/block")
    fun blockEventProfileById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
    ): Mono<EventProfileReaderDto>

    @Operation(
        summary = "Unblock Event's Profile",
        description = "Re-authorize a User to use it",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_PROFILE_U')")
    @PatchMapping("/{id}/unblock")
    fun unblockEventProfileById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable eventId: UUID,
        @PathVariable id: UUID,
    ): Mono<EventProfileReaderDto>

    @Operation(
        summary = "Delete Event's Profile",
        description = "Delete Event's Profile",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_PROFILE_D')")
    @DeleteMapping("/{id}")
    fun deleteEventProfileById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @PathVariable eventId: UUID,
        @PathVariable id: UUID
    ): Mono<Void>
}
