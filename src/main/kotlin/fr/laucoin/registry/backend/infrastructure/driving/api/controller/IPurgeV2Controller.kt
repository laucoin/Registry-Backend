package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.annotation.RateLimited
import fr.laucoin.registry.backend.domain.constant.ApiConst.API_V2
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PurgeError.PURGE_DATE_THRESHOLD_IN_FUTURE
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_JOB_C
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SENSITIVE
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectConfigurationPurgeReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectContentPurgeReaderDto
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
@RequestMapping("$API_V2/purge")
interface IPurgeV2Controller {
	@Operation(
		summary = "Purge users",
		description = """
			Permanently delete Users disabled for longer than the given (or default) threshold. Intended to be called by
			the scheduled purge job; use `dryRun=true` (the default) to preview the IDs that would be deleted without deleting anything.
		""",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_JOB_C')")
	@RateLimited(SENSITIVE)
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
		description = """
			Permanently delete Projects disabled for longer than the given (or default) threshold. Intended to be called by
			the scheduled purge job; use `dryRun=true` (the default) to preview the IDs that would be deleted without deleting anything.
		""",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_JOB_C')")
	@RateLimited(SENSITIVE)
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
		description = """
			Permanently delete the content (Movements, Communications and Alerts) disabled for longer than the given
			(or default) threshold, across every Project. Intended to be called by the scheduled purge job; use `dryRun=true`
			(the default) to preview the counts that would be deleted without deleting anything.
		""",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_JOB_C')")
	@RateLimited(SENSITIVE)
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
		description = """
			Permanently delete the configuration (Vehicles, Activities, Groups and Participants) disabled for longer than
			the given (or default) threshold, across every Project. Intended to be called by the scheduled purge job; use `dryRun=true`
			(the default) to preview the counts that would be deleted without deleting anything.
		""",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_JOB_C')")
	@RateLimited(SENSITIVE)
	@PostMapping("/projects/configurations")
	fun purgeProjectsConfigurationsIfNecessary(
		@Parameter(description = "If not specified, the purge will be done with the default threshold defined in registry's configuration")
		@RequestParam(required = false)
		@Valid @PastOrPresent(message = PURGE_DATE_THRESHOLD_IN_FUTURE)
		@DateTimeFormat(iso = DATE) dateThreshold: LocalDate? = null,
		@RequestParam(required = false, defaultValue = "true") dryRun: Boolean,
	): Mono<ProjectConfigurationPurgeReaderDto>
}
