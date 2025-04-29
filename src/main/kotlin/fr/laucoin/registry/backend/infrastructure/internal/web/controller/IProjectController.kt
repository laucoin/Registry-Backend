package fr.laucoin.registry.backend.infrastructure.internal.web.controller

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_U
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_PROJECT_C
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_PROJECT_METADATA_R
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ProjectOptionsReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ProjectReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.ProjectWriterDto
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

@Tag(name = "Projects management", description = "API for Projects-related operations")
@RequestMapping("/api/projects")
interface IProjectController {
    @Operation(
        summary = "Find Projects",
        description = "Find or get paginated Projects",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @GetMapping
    fun findProjects(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @RequestParam(defaultValue = "0") @Valid @Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO) pageNumber: Int,
        @RequestParam(defaultValue = "20") @Valid @Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE) @Max(
            200,
            message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
        ) pageSize: Int,
        @RequestParam(required = false) textSearched: String?,
        @RequestParam(required = false) visibilitySearched: Boolean?,
        @Parameter(description = "\"false\" value will be considered only if you have REGISTRY_PROJECT_R authority.")
        @RequestParam(required = false, defaultValue = "true") withProfile: Boolean,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DATE_TIME) dateTimeSearched: ZonedDateTime?,
    ): Mono<PageModel<ProjectReaderDto>>

    @Operation(
        summary = "Find Project",
        description = "Find Project by ID",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasAuthority('${UserPermissionConst.REGISTRY_PROJECT_R}') || hasPermission(#id, '${ProjectPermissionConst.REGISTRY_PROJECT_R}')")
    @GetMapping("/{id}")
    fun findProjectById(@RequestHeader(ACCEPT_LANGUAGE) locale: Locale, @PathVariable id: UUID): Mono<ProjectReaderDto>

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
    @PreAuthorize("hasAuthority('$REGISTRY_PROJECT_METADATA_R')")
    @GetMapping("/options")
    fun getAvailableProjectOptions(@RequestHeader(ACCEPT_LANGUAGE) locale: Locale): Flux<ProjectOptionsReaderDto>

    @Operation(
        summary = "Create Project",
        description = "Create Project and Project Profile administration for the Current User",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasAuthority('$REGISTRY_PROJECT_C')")
    @PostMapping
    fun createProject(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @RequestBody @Valid project: ProjectWriterDto,
    ): Mono<ProjectReaderDto>

    @Operation(
        summary = "Update Project",
        description = "Update Project",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#id, '$REGISTRY_PROJECT_U')")
    @PatchMapping("/{id}")
    fun updateProjectById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable id: UUID,
        @RequestBody @Valid project: ProjectWriterDto
    ): Mono<ProjectReaderDto>

    @Operation(
        summary = "Disable Project",
        description = "Disable Project access, obviously the related profile is no accessible anymore.",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#id, '$REGISTRY_PROJECT_U')")
    @PatchMapping("/{id}/disable")
    fun disableProjectById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable id: UUID,
    ): Mono<ProjectReaderDto>

    @Operation(
        summary = "Enable Project",
        description = "Enable Project, obviously the profiles concerned are accessible again.",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#id, '$REGISTRY_PROJECT_U')")
    @PatchMapping("/{id}/enable")
    fun enableProjectById(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @RequestHeader(ACCEPT_LANGUAGE) locale: Locale,
        @PathVariable id: UUID,
    ): Mono<ProjectReaderDto>

    @Operation(
        summary = "Delete Project",
        description = "Delete all Project data.",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PreAuthorize("hasPermission(#id, '$REGISTRY_PROJECT_D')")
    @DeleteMapping("/{id}")
    fun deleteProjectById(@PathVariable id: UUID): Mono<Void>
}
