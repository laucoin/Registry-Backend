package fr.laucoin.registry.backend.infrastructure.internal.web.controller

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_PROFILE_C
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ProjectProfileReaderDto
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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Mono

@Tag(name = "User's Profiles management", description = "API for User's Profiles-related operations")
@RequestMapping("/api/users/profiles")
interface IUserProjectProfileController {
    @Operation(
        summary = "Find User's Profiles",
        description = "Find or get paginated User's Profiles",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @GetMapping
    fun findUserProjectProfiles(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
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
    ): Mono<PageModel<ProjectProfileReaderDto>>

    @Operation(
        summary = "Find User's Profile",
        description = "Find User's Profile by ID",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @GetMapping("/{id}")
    fun findUserProjectProfileById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable id: UUID,
    ): Mono<ProjectProfileReaderDto>

    @Operation(
        summary = "Accept or Reject Project's invitation",
        description = "Allow User to access the concerned Project if accepted",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PostMapping("/{id}/accept/{accepted}")
    fun manageUserProjectProfileAcceptance(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable id: UUID,
        @PathVariable accepted: Boolean,
    ): Mono<ProjectProfileReaderDto>

    @Operation(
        summary = "Create support Project's Profile",
        description = "Support profile is a temporary Profile for an User to access an Project to help the administration",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasAuthority('$REGISTRY_PROFILE_C')")
    @PostMapping("/{projectId}/support")
    fun createSupportProjectProfile(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable projectId: UUID,
    ): Mono<ProjectProfileReaderDto>

    @Operation(
        summary = "Delete User's Profile",
        description = "Delete User's Profile",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @DeleteMapping("/{id}")
    fun deleteUserProfileById(@AuthenticationPrincipal currentUser: CurrentUserModel, @PathVariable id: UUID): Mono<Void>
}
