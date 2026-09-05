package fr.laucoin.registry.backend.infrastructure.out.api.controller

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PurgeError.PURGE_DATE_THRESHOLD_IN_FUTURE
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_JOB_C
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectConfigurationPurgeReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectContentPurgeReaderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.PastOrPresent
import java.time.LocalDate
import java.util.UUID
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Tag(name = "Scheduled job management", description = "API for Scheduled job management")
@RequestMapping("/api/v1/purge")
interface IPurgeV1Controller {
	@Operation(
		summary = "Purge users",
		description = "Purge users if necessary",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_JOB_C')")
	@PostMapping("/users")
	fun purgeUsersIfNecessary(
		@Parameter(description = "If not specified, the purge will be done with the default threshold defined in registry's configuration")
		@RequestParam(required = false)
		@Valid @PastOrPresent(message = PURGE_DATE_THRESHOLD_IN_FUTURE)
		@DateTimeFormat(iso = DATE) dateThreshold: LocalDate? = null,
		@RequestParam(required = false, defaultValue = "true") dryRun: Boolean,
	): Flux<UUID>

	@Operation(
		summary = "Purge projects",
		description = "Purge projects if necessary",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_JOB_C')")
	@PostMapping("/projects")
	fun purgeProjectsIfNecessary(
		@Parameter(description = "If not specified, the purge will be done with the default threshold defined in registry's configuration")
		@RequestParam(required = false)
		@Valid @PastOrPresent(message = PURGE_DATE_THRESHOLD_IN_FUTURE)
		@DateTimeFormat(iso = DATE) dateThreshold: LocalDate? = null,
		@RequestParam(required = false, defaultValue = "true") dryRun: Boolean,
	): Flux<UUID>

	@Operation(
		summary = "Purge projects contents",
		description = "Purge projects contents (movements, communications and alerts) if necessary",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_JOB_C')")
	@PostMapping("/projects/contents")
	fun purgeProjectsContentsIfNecessary(
		@Parameter(description = "If not specified, the purge will be done with the default threshold defined in registry's configuration")
		@RequestParam(required = false)
		@Valid @PastOrPresent(message = PURGE_DATE_THRESHOLD_IN_FUTURE)
		@DateTimeFormat(iso = DATE) dateThreshold: LocalDate? = null,
		@RequestParam(required = false, defaultValue = "true") dryRun: Boolean,
	): Mono<ProjectContentPurgeReaderDto>

	@Operation(
		summary = "Purge projects configurations",
		description = "Purge projects configurations (vehicles, activities, groups and participants) if necessary",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_JOB_C')")
	@PostMapping("/projects/configurations")
	fun purgeProjectsConfigurationsIfNecessary(
		@Parameter(description = "If not specified, the purge will be done with the default threshold defined in registry's configuration")
		@RequestParam(required = false)
		@Valid @PastOrPresent(message = PURGE_DATE_THRESHOLD_IN_FUTURE)
		@DateTimeFormat(iso = DATE) dateThreshold: LocalDate? = null,
		@RequestParam(required = false, defaultValue = "true") dryRun: Boolean,
	): Mono<ProjectConfigurationPurgeReaderDto>
}
