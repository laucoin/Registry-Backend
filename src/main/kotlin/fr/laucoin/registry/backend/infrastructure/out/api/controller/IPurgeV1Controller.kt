package fr.laucoin.registry.backend.infrastructure.out.api.controller

import fr.laucoin.registry.backend.domain.annotation.RateLimited
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_JOB_C
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SENSITIVE
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectConfigurationPurgeReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectContentPurgeReaderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

@Deprecated(
	"API v1 has no remaining Registry-Frontend consumer and is scheduled for removal; use the /api/v2 contract.",
	level = DeprecationLevel.WARNING,
)
@Tag(name = "Scheduled job management (v1, deprecated)", description = "API for Scheduled job management — deprecated, scheduled for removal; use /api/v2")
@RequestMapping("/api/v1/purge")
interface IPurgeV1Controller {
	@Operation(
		summary = "Purge users",
		description = "Purge users if necessary",
		deprecated = true,
	)
	@PreAuthorize("hasAuthority('$REGISTRY_JOB_C')")
	@RateLimited(SENSITIVE)
	@PostMapping("/users")
	fun purgeUsersIfNecessary(
		@Parameter(description = "If not specified, the purge will be done with the default threshold defined in registry's configuration")
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE) dateThreshold: LocalDate? = null,
		@RequestParam(required = false, defaultValue = "true") dryRun: Boolean,
	): Flux<UUID>

	@Operation(
		summary = "Purge light users",
		description = "Purge stale light users (email-only invitations never claimed by a first login) if necessary",
		deprecated = true,
	)
	@PreAuthorize("hasAuthority('$REGISTRY_JOB_C')")
	@RateLimited(SENSITIVE)
	@PostMapping("/light-users")
	fun purgeLightUsersIfNecessary(
		@Parameter(description = "If not specified, the purge will be done with the default threshold defined in registry's configuration")
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE) dateThreshold: LocalDate? = null,
		@RequestParam(required = false, defaultValue = "true") dryRun: Boolean,
	): Flux<UUID>

	@Operation(
		summary = "Purge projects",
		description = "Purge projects if necessary",
		deprecated = true,
	)
	@PreAuthorize("hasAuthority('$REGISTRY_JOB_C')")
	@RateLimited(SENSITIVE)
	@PostMapping("/projects")
	fun purgeProjectsIfNecessary(
		@Parameter(description = "If not specified, the purge will be done with the default threshold defined in registry's configuration")
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE) dateThreshold: LocalDate? = null,
		@RequestParam(required = false, defaultValue = "true") dryRun: Boolean,
	): Flux<UUID>

	@Operation(
		summary = "Purge projects contents",
		description = "Purge projects contents (movements, communications and alerts) if necessary",
		deprecated = true,
	)
	@PreAuthorize("hasAuthority('$REGISTRY_JOB_C')")
	@RateLimited(SENSITIVE)
	@PostMapping("/projects/contents")
	fun purgeProjectsContentsIfNecessary(
		@Parameter(description = "If not specified, the purge will be done with the default threshold defined in registry's configuration")
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE) dateThreshold: LocalDate? = null,
		@RequestParam(required = false, defaultValue = "true") dryRun: Boolean,
	): Mono<ProjectContentPurgeReaderDto>

	@Operation(
		summary = "Purge projects configurations",
		description = "Purge projects configurations (vehicles, activities, groups and participants) if necessary",
		deprecated = true,
	)
	@PreAuthorize("hasAuthority('$REGISTRY_JOB_C')")
	@RateLimited(SENSITIVE)
	@PostMapping("/projects/configurations")
	fun purgeProjectsConfigurationsIfNecessary(
		@Parameter(description = "If not specified, the purge will be done with the default threshold defined in registry's configuration")
		@RequestParam(required = false)
		@DateTimeFormat(iso = DATE) dateThreshold: LocalDate? = null,
		@RequestParam(required = false, defaultValue = "true") dryRun: Boolean,
	): Mono<ProjectConfigurationPurgeReaderDto>
}
