package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AlertCriticalityMetadataReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectOptionsReaderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Purpose: REST contract of the metadata endpoints under /api/v2.
 * Scope: Carries the routes, authorization rules, validation bounds and OpenAPI metadata.
 * Limits: Declares no behavior; the controller implementation delegates to the services.
 */
@Tag(name = "Metadata", description = "API for global metadata")
@RequestMapping("/api/v2/metadata")
interface IMetadataV2Controller {

	@Operation(
		summary = "Get presence element's status",
		description = "Get all presence element's status",
	)
	@GetMapping("/presences/status")
	fun getPresencesStatus(): Flux<LabelDto>

	@Operation(
		summary = "Get availabilities status",
		description = "Get all availabilities status",
	)
	@GetMapping("/availabilities/status")
	fun getAvailabilitiesStatus(): Flux<LabelDto>

	@Operation(
		summary = "Get profile's status",
		description = "Get all profile's status",
	)
	@GetMapping("/profiles/status")
	fun getProjectProfileStatus(): Flux<LabelDto>

	@Operation(
		summary = "Get Movement Type",
		description = "Get all movement type",
	)
	@GetMapping("/movements/types")
	fun getMovementTypes(): Flux<LabelDto>

	@Operation(
		summary = "Get Participant Type",
		description = "Get all participant type",
	)
	@GetMapping("/participants/types")
	fun getParticipantTypes(): Flux<LabelDto>

	@Operation(
		summary = "Get Alert Status",
		description = "Get all alert status",
	)
	@GetMapping("/alerts/status")
	fun getAlertStatus(): Flux<LabelDto>

	@Operation(
		summary = "Get Alert Criticality",
		description = "Get all alert criticality levels with the sentence describing how they are computed from the alert age",
	)
	@GetMapping("/alerts/criticality")
	fun getAlertCriticality(): Mono<AlertCriticalityMetadataReaderDto>

	@Operation(
		summary = "Get available Project's Options",
		description = "Get all the Options a Project can enable",
	)
	@GetMapping("/projects/options")
	fun getAvailableProjectOptions(): Flux<ProjectOptionsReaderDto>
}
