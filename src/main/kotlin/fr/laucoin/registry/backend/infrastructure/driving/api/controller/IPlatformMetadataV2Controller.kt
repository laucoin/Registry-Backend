package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.FeaturesReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectOptionsReaderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat REST des métadonnées de la plateforme : options de projet disponibles et fonctionnalités activées.
 * Périmètre : Porte les routes de lecture, leurs autorisations et leurs métadonnées OpenAPI.
 * Limites : Ne déclare aucun comportement ; l'implémentation du contrôleur délègue aux services.
 */
@Tag(name = "Metadata", description = "API for global metadata")
@RequestMapping("/api/v2/metadata")
interface IPlatformMetadataV2Controller {
	@Operation(
		summary = "Get available Project's Options",
		description = "Get all the Options a Project can enable",
	)
	@GetMapping("/projects/options")
	fun getAvailableProjectOptions(): Flux<ProjectOptionsReaderDto>

	@Operation(
		summary = "Get enabled Features",
		description = "Get the state of the features enabled by configuration",
	)
	@GetMapping("/features")
	fun getFeatures(): Mono<FeaturesReaderDto>
}
