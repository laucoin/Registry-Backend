package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.constant.ApiConst.API_V2
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import reactor.core.publisher.Flux
import java.security.Principal

@Tag(name = "Metadata", description = "API for global metadata")
@RequestMapping("$API_V2/metadata")
interface IMetadataV2Controller {

	@Operation(
		summary = "Get presence element's status",
		description = "Get all presence element's status",
	)
	@GetMapping("/presences/status")
	fun getPresencesStatus(principal: Principal): Flux<LabelDto>

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
}
