package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ActivityQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ActivityMovementQueryDto
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum
import fr.laucoin.registry.backend.domain.service.support.IConcurrencyService
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.service.usecase.IActivityUseCase
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IActivityV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ActivityReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ActivityStatisticsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ActivityWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.ETagExt.expectedLastEditions
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.LocationExt.created
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query.ActivityQueryDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ActivityReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ActivityStatisticsReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.ActivityWriterDtoMapper
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_RESTORE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_RESTORE

/**
 * Objectif : Adaptateur REST des endpoints d'activité du projet.
 * Périmètre : Mappe les modèles d'activité vers des DTO et délègue à IActivityUseCase.
 * Limites : Ne porte aucune règle métier.
 */
@RestController
class ActivityV2Controller(
	private val service: IActivityUseCase,
	private val concurrencyService: IConcurrencyService,
	private val readerMapper: ActivityReaderDtoMapper,
	private val statisticsReaderMapper: ActivityStatisticsReaderDtoMapper,
	private val movementReaderMapper: MovementReaderDtoMapper,
	private val writerMapper: ActivityWriterDtoMapper,
	private val queryMapper: ActivityQueryDtoMapper,
): IActivityV2Controller {
	override fun findActivities(
		currentUser: CurrentUserModel,
		projectId: UUID,
		search: ActivityQueryDto,
	): Mono<PageModel<ActivityReaderDto>> {
		val visibility = currentUser.visibilityFor(search.isVisible, projectId, REGISTRY_PROJECT_ACTIVITY_RESTORE)
		return service.findActivitiesPage(projectId, queryMapper.toPageable(search), queryMapper.toSearchParams(search, visibility))
			.map(readerMapper::toDtoPage)
	}

	override fun findActivityById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<ActivityReaderDto> {
		return service.findActivityById(projectId, id, isVisible = currentUser.visibilityFor(null, projectId, REGISTRY_PROJECT_ACTIVITY_RESTORE)).map(readerMapper::toDto)
	}

	override fun findActivityStatistics(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<ActivityStatisticsReaderDto> {
		return service.findActivityStatistics(projectId, id).map(statisticsReaderMapper::toDto)
	}

	override fun findActivityMovements(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		search: ActivityMovementQueryDto,
	): Mono<PageModel<MovementReaderDto>> {
		val visibility = currentUser.visibilityFor(search.isVisible, projectId, REGISTRY_PROJECT_MOVEMENT_RESTORE)
		val searchParams = queryMapper.toMovementSearchParams(search, visibility)

		return service.findActivityMovementsPage(projectId, id, queryMapper.toPageable(search), searchParams)
			.map(movementReaderMapper::toDtoPage)
	}

	override fun createActivity(
		currentUser: CurrentUserModel,
		projectId: UUID,
		activity: ActivityWriterDto,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<ActivityReaderDto>> {
		val activityModel = writerMapper.toModel(activity, projectId)
		return service.createActivity(currentUser, activityModel)
			.map(readerMapper::toDto)
			.map { exchange.created(it) }
	}

	override fun updateActivityById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		activity: ActivityWriterDto,
		exchange: ServerWebExchange,
	): Mono<ActivityReaderDto> {
		val activityModel = writerMapper.toModel(activity, projectId)
		return concurrencyService.updateIfUnchanged(
			LockableResourceEnum.ACTIVITY,
			id,
			exchange.expectedLastEditions(),
			current = { service.findActivityById(projectId, id, isVisible = null) },
			action = { service.updateActivityById(currentUser, projectId, id, activityModel) },
		).map(readerMapper::toDto)
	}

	override fun disableActivityById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<ActivityReaderDto> {
		return service.disableActivityById(currentUser, projectId, id).map(readerMapper::toDto)
	}

	override fun enableActivityById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<ActivityReaderDto> {
		return service.enableActivityById(currentUser, projectId, id).map(readerMapper::toDto)
	}

	override fun deleteActivityById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Unit> {
		return service.deleteActivityById(currentUser, projectId, id)
	}
}
