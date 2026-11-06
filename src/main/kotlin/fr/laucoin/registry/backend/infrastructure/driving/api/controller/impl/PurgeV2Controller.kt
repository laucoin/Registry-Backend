package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PURGE_REPORTED_IDS_MAX
import fr.laucoin.registry.backend.domain.service.usecase.IPlatformPurgeUseCase
import fr.laucoin.registry.backend.domain.service.usecase.IProjectConfigurationPurgeUseCase
import fr.laucoin.registry.backend.domain.service.usecase.IProjectContentPurgeUseCase
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IPurgeV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AuditPurgeReaderDto
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
 * Objectif : Adaptateur REST des endpoints de purge.
 * Périmètre : Mappe les paramètres de requête et les DTO, et délègue aux cas d'usage de purge.
 * Limites : Ne porte aucune règle métier ; l'autorisation est déclarée sur le contrat.
 */
@RestController
class PurgeV2Controller(
	private val platform: IPlatformPurgeUseCase,
	private val content: IProjectContentPurgeUseCase,
	private val configuration: IProjectConfigurationPurgeUseCase,
	private val contentMapper: ProjectContentPurgeReaderDtoMapper,
	private val configurationMapper: ProjectConfigurationPurgeReaderDtoMapper,
) : IPurgeV2Controller {
	private fun Flux<UUID>.cappedForReport(): Flux<UUID> {
		return collectList().flatMapMany { Flux.fromIterable(it.take(PURGE_REPORTED_IDS_MAX)) }
	}

	override fun purgeUsersIfNecessary(dateThreshold: LocalDate?, dryRun: Boolean): Flux<UUID> {
		return platform.purgeUsers(dateThreshold, dryRun).cappedForReport()
	}

	override fun purgeProjectsIfNecessary(dateThreshold: LocalDate?, dryRun: Boolean): Flux<UUID> {
		return platform.purgeProjects(dateThreshold, dryRun).cappedForReport()
	}

	override fun purgeProjectsContentsIfNecessary(
		dateThreshold: LocalDate?,
		dryRun: Boolean,
	): Mono<ProjectContentPurgeReaderDto> {
		return content.purgeProjectsContents(dateThreshold, dryRun).map(contentMapper::toDto)
	}

	override fun purgeProjectsConfigurationsIfNecessary(
		dateThreshold: LocalDate?,
		dryRun: Boolean,
	): Mono<ProjectConfigurationPurgeReaderDto> {
		return configuration.purgeProjectsConfigurations(dateThreshold, dryRun).map(configurationMapper::toDto)
	}

	override fun purgeAuditsIfNecessary(dateThreshold: LocalDate?, dryRun: Boolean): Mono<AuditPurgeReaderDto> {
		return platform.purgeAudits(dateThreshold, dryRun).map { AuditPurgeReaderDto(it) }
	}
}
