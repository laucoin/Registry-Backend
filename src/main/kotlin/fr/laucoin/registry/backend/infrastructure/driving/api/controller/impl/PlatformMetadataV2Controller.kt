package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.service.support.IFeatureService
import fr.laucoin.registry.backend.domain.service.usecase.IProjectUseCase
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IPlatformMetadataV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.FeaturesReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectOptionsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.FeaturesReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ProjectOptionsReaderDtoMapper
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Adaptateur REST des métadonnées de la plateforme (options de projet, fonctionnalités).
 * Périmètre : Mappe les options et les fonctionnalités en DTO.
 * Limites : Ne porte aucune règle métier ; l'autorisation est déclarée sur le contrat.
 */
@RestController
class PlatformMetadataV2Controller(
	private val projectService: IProjectUseCase,
	private val projectOptionsReaderMapper: ProjectOptionsReaderDtoMapper,
	private val featureService: IFeatureService,
	private val featuresReaderMapper: FeaturesReaderDtoMapper,
): IPlatformMetadataV2Controller {
	override fun getAvailableProjectOptions(): Flux<ProjectOptionsReaderDto> {
		return projectService.availableProjectOptions().map(projectOptionsReaderMapper::toDto)
	}

	override fun getFeatures(): Mono<FeaturesReaderDto> {
		return Mono.fromSupplier { featuresReaderMapper.toDto(featureService.isInviteByEmailEnabled()) }
	}
}
