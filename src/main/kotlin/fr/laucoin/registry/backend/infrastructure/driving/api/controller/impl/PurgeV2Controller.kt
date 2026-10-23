package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.service.IPurgeService
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IPurgeV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectConfigurationPurgeReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectContentPurgeReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ProjectConfigurationPurgeReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ProjectContentPurgeReaderDtoMapper
import java.time.LocalDate
import java.util.UUID
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Purpose: REST adapter of the purge endpoints.
 * Scope: Maps request parameters and DTOs, and delegates to the purge use cases.
 * Limits: Holds no business rule; authorization is declared on the contract.
 */
@RestController
class PurgeV2Controller(
	private val service: IPurgeService,
	private val contentMapper: ProjectContentPurgeReaderDtoMapper,
	private val configurationMapper: ProjectConfigurationPurgeReaderDtoMapper,
) : IPurgeV2Controller {
	override fun purgeUsersIfNecessary(dateThreshold: LocalDate?, dryRun: Boolean): Flux<UUID> {
		return service.purgeUsers(dateThreshold, dryRun)
	}

	override fun purgeProjectsIfNecessary(dateThreshold: LocalDate?, dryRun: Boolean): Flux<UUID> {
		return service.purgeProjects(dateThreshold, dryRun)
	}

	override fun purgeProjectsContentsIfNecessary(
		dateThreshold: LocalDate?,
		dryRun: Boolean,
	): Mono<ProjectContentPurgeReaderDto> {
		return service.purgeProjectsContents(dateThreshold, dryRun).map(contentMapper::toDto)
	}

	override fun purgeProjectsConfigurationsIfNecessary(
		dateThreshold: LocalDate?,
		dryRun: Boolean,
	): Mono<ProjectConfigurationPurgeReaderDto> {
		return service.purgeProjectsConfigurations(dateThreshold, dryRun).map(configurationMapper::toDto)
	}
}
