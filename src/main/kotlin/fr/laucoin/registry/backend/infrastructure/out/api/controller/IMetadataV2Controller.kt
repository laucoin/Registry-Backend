package fr.laucoin.registry.backend.infrastructure.out.api.controller

import fr.laucoin.registry.backend.domain.annotation.HttpCacheable
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.FeaturesReaderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import reactor.core.publisher.Flux

/**
 * API v2 Metadata contract (ADR 017) — localized label lookups for the
 * frontend. Authenticated; no special permission (same as v1).
 */
@Tag(name = "Metadata", description = "API for global metadata")
@RequestMapping("/api/v2/metadata")
interface IMetadataV2Controller {

	@Operation(
		summary = "Get presence element's status",
		description = "Get all presence element's status",
	)
	@HttpCacheable
	@GetMapping("/presences/status")
	fun getPresencesStatus(): Flux<LabelDto>

	@Operation(
		summary = "Get availabilities status",
		description = "Get all availabilities status",
	)
	@HttpCacheable
	@GetMapping("/availabilities/status")
	fun getAvailabilitiesStatus(): Flux<LabelDto>

	@Operation(
		summary = "Get profile's status",
		description = "Get all profile's status",
	)
	@HttpCacheable
	@GetMapping("/profiles/status")
	fun getProjectProfileStatus(): Flux<LabelDto>

	@Operation(
		summary = "Get Movement Type",
		description = "Get all movement type",
	)
	@HttpCacheable
	@GetMapping("/movements/types")
	fun getMovementTypes(): Flux<LabelDto>

	@Operation(
		summary = "Get Participant Type",
		description = "Get all participant type",
	)
	@HttpCacheable
	@GetMapping("/participants/types")
	fun getParticipantTypes(): Flux<LabelDto>

	@Operation(
		summary = "Get Alert Status",
		description = "Get all alert status",
	)
	@HttpCacheable
	@GetMapping("/alerts/status")
	fun getAlertStatus(): Flux<LabelDto>

	@Operation(
		summary = "Get feature switches",
		description = "Get the deployment feature switches the UI mirrors",
	)
	@HttpCacheable
	@GetMapping("/features")
	fun getFeatures(): FeaturesReaderDto
}
