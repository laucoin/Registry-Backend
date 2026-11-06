package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import org.springdoc.core.annotations.ParameterObject
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ActivityQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ActivityMovementQueryDto
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_HISTORY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_RESTORE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_SOFT_DELETE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_U
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ACTIVITY
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ActivityReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ActivityStatisticsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ActivityWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat REST des endpoints de l'activité sous /api/v2.
 * Périmètre : Porte les routes, les règles d'autorisation, les bornes de validation et les métadonnées OpenAPI.
 * Limites : Ne déclare aucun comportement ; l'implémentation du contrôleur délègue aux services.
 */
@Tag(name = "Activities management", description = "API for Activities-related operations")
@RequestMapping("/api/v2/projects/{projectId}/activities")
interface IActivityV2Controller {
	@Operation(
		summary = "Find Activities",
		description = "Find or get paginated Activities",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_ACTIVITY_R')")
	@GetMapping
	fun findActivities(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@Valid @ParameterObject search: ActivityQueryDto,
	): Mono<PageModel<ActivityReaderDto>>

	@Operation(
		summary = "Find Activity",
		description = "Find Activity by ID",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_ACTIVITY_R')")
	@GetMapping("/{id}")
	fun findActivityById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<ActivityReaderDto>

	@Operation(
		summary = "Find Activity statistics",
		description = "Statistics of the visible OUT movements of the Activity: the number of distinct participants who took part, the number of participants of the Project (visible registered ones) and their rate (null when the Project has none), the time spent out in seconds counting each movement from its departure to the last return of its participants, and the man time (the sum of the time each participant spent out). A participant who has not come back is counted until now",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_ACTIVITY_HISTORY_R')")
	@GetMapping("/{id}/statistics")
	fun findActivityStatistics(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<ActivityStatisticsReaderDto>

	@Operation(
		summary = "Find Activity Movements",
		description = "Find or get paginated activity Movements",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_ACTIVITY_HISTORY_R')")
	@GetMapping("/{id}/movements")
	fun findActivityMovements(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@Valid @ParameterObject search: ActivityMovementQueryDto,
	): Mono<PageModel<MovementReaderDto>>

	@Operation(
		summary = "Create Activity",
		description = "Create Activity linked to the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_ACTIVITY_C')")
	@PostMapping
	fun createActivity(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@RequestBody @Valid activity: ActivityWriterDto,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<ActivityReaderDto>>

	@Operation(
		summary = "Update Activity",
		description = "Update Activity",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_ACTIVITY_U')")
	@PatchMapping("/{id}")
	fun updateActivityById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid activity: ActivityWriterDto,
		exchange: ServerWebExchange,
	): Mono<ActivityReaderDto>

	@Operation(
		summary = "Disable Activity",
		description = "Disable Activity, it will not visible anymore in the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_ACTIVITY_SOFT_DELETE')")
	@PostMapping("/{id}/disable")
	fun disableActivityById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<ActivityReaderDto>

	@Operation(
		summary = "Enable Activity",
		description = "Enable Activity, obviously it will be visible again in the Project",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_ACTIVITY_RESTORE')")
	@PostMapping("/{id}/enable")
	fun enableActivityById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<ActivityReaderDto>

	@Operation(
		summary = "Delete Activity",
		description = "Delete all Activity data.",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_ACTIVITY_D')")
	@DeleteMapping("/{id}")
	@ResponseStatus(NO_CONTENT)
	fun deleteActivityById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
	): Mono<Unit>
}
