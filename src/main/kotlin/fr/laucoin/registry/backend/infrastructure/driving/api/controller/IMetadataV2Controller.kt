package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.constant.ApiConst.API_V2
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectOptionsReaderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import java.security.Principal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import reactor.core.publisher.Flux

@Tag(name = "Metadata", description = "API for global metadata")
@RequestMapping("$API_V2/metadata")
interface IMetadataV2Controller {

	@Operation(
		summary = "Get available Options",
		description = """
			List every Project option (VEHICLE, ACTIVITY, COMMUNICATION, ALERT) the caller is allowed to enable when
			creating or updating a Project, including each option's required dependencies (e.g. ALERT requires ACTIVITY and COMMUNICATION).
		""",
	)
	@GetMapping("/projects/options")
	fun getAvailableProjectOptions(): Flux<ProjectOptionsReaderDto>

	@Operation(
		summary = "Get presence element's status",
		description = "List the presence statuses (e.g. IN, OUT, UNAVAILABLE) with their labels, localized for the caller.",
	)
	@GetMapping("/presences/status")
	fun getPresencesStatus(principal: Principal): Flux<LabelDto>

	@Operation(
		summary = "Get availabilities status",
		description = """
			List the availability statuses used to describe whether a Participant, Group or Vehicle is currently allowed
			to be present, with their labels localized for the caller.
		""",
	)
	@GetMapping("/availabilities/status")
	fun getAvailabilitiesStatus(): Flux<LabelDto>

	@Operation(
		summary = "Get profile's status",
		description = "List the Project Profile statuses (INVITED, ACCEPTED, REJECTED, BLOCKED) with their labels localized for the caller.",
	)
	@GetMapping("/profiles/status")
	fun getProjectProfileStatus(): Flux<LabelDto>

	@Operation(
		summary = "Get Movement Type",
		description = "List the Movement types (IN, OUT) with their labels localized for the caller.",
	)
	@GetMapping("/movements/types")
	fun getMovementTypes(): Flux<LabelDto>

	@Operation(
		summary = "Get Participant Type",
		description = "List the Participant types (REGISTERED, GUEST) with their labels localized for the caller.",
	)
	@GetMapping("/participants/types")
	fun getParticipantTypes(): Flux<LabelDto>

	@Operation(
		summary = "Get Alert Status",
		description = "List the Alert statuses (IN_PROGRESS, RESOLVED, CANCELED) with their labels localized for the caller.",
	)
	@GetMapping("/alerts/status")
	fun getAlertStatus(): Flux<LabelDto>
}
