package fr.laucoin.registry.backend.infrastructure.internal.web.controller

import fr.laucoin.registry.backend.domain.annotation.ProfileAcceptOrReject
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.EventProfileReaderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Mono

@Tag(name = "User's Profiles management", description = "API for User's Profiles-related operations")
@RequestMapping("/api/users/profiles")
interface IUserEventProfileController {
    @Operation(
        summary = "Find User's Profiles",
        description = "Find or get paginated User's Profiles"
    )
    @GetMapping
    fun findUserEventProfiles(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "ASC") order: Direction,
        @RequestParam(defaultValue = "true") onlyVisible: Boolean,
        @RequestParam(defaultValue = "true") onlyUsable: Boolean,
        @RequestParam(required = false) status: ProfileStatusEnum?,
        @RequestParam(required = false) searched: String?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DATE_TIME) startAccess: ZonedDateTime?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DATE_TIME) endAccess: ZonedDateTime?,
    ): Mono<PageDto<EventProfileReaderDto>>

    @Operation(
        summary = "Find User's Profile",
        description = "Find User's Profile by ID"
    )
    @GetMapping("/{id}")
    fun findUserEventProfileById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @PathVariable id: UUID,
    ): Mono<EventProfileReaderDto>

    @Operation(
        summary = "Accept or Reject Event's invitation",
        description = "Allow User to access the concerned Event if accepted"
    )
    @PostMapping("/{id}/status/{status}")
    fun manageUserEventProfileAcceptance(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @PathVariable id: UUID,
        @PathVariable @ProfileAcceptOrReject @Valid status: ProfileStatusEnum,
    ): Mono<EventProfileModel>

    @Operation(
        summary = "Delete User's Profile",
        description = "Delete User's Profile"
    )
    @DeleteMapping("/{id}")
    fun deleteUserProfileById(@AuthenticationPrincipal currentUser: CurrentUserModel, @PathVariable id: UUID): Mono<Void>
}
