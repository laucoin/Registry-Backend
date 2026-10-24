package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum
import fr.laucoin.registry.backend.domain.service.IConcurrencyService
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_DURATION_FORMAT_FAILED
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.extension.SortExt.toSortModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.service.IActivityService
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IActivityV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ActivityReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ActivityWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.ETagExt.expectedLastEditions
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.LocationExt.created
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ActivityReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.ActivityWriterDtoMapper
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_D

/**
 * Purpose: REST adapter of the project activity endpoints.
 * Scope: Maps activity models to DTOs and delegates to IActivityService.
 * Limits: Holds no business rule.
 */
@RestController
class ActivityV2Controller(
	private val service: IActivityService,
	private val concurrencyService: IConcurrencyService,
	private val readerMapper: ActivityReaderDtoMapper,
	private val movementReaderMapper: MovementReaderDtoMapper,
	private val writerMapper: ActivityWriterDtoMapper,
): IActivityV2Controller {
	override fun findActivities(
		currentUser: CurrentUserModel,
		projectId: UUID,
		page: Int,
		size: Int,
		query: String?,
		isVisible: Boolean?,
		isAvailable: Boolean?,
		at: ZonedDateTime?,
		minDuration: String?,
		maxDuration: String?,
		participants: Int?,
		sort: String?,
	): Mono<PageModel<ActivityReaderDto>> {
		val visibility = currentUser.visibilityFor(isVisible, projectId, REGISTRY_PROJECT_ACTIVITY_D)
		val pageable = PageableModel(page * size, size)
		val searchParams = ActivitySearchParamModel(
			query, visibility, isAvailable, at,
			parseDuration(minDuration), parseDuration(maxDuration), participants,
			sort.toSortModel(ActivitySearchParamModel.SORT_FIELDS),
		)

		return service.findActivitiesPage(projectId, pageable, searchParams).map(readerMapper::toDtoPage)
	}

	override fun findActivityById(projectId: UUID, id: UUID): Mono<ActivityReaderDto> {
		return service.findActivityById(projectId, id, isVisible = null).map(readerMapper::toDto)
	}

	override fun findActivityMovements(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		page: Int,
		size: Int,
		isVisible: Boolean?,
		type: MovementTypeEnum?,
		from: ZonedDateTime?,
		to: ZonedDateTime?,
		sort: String?,
	): Mono<PageModel<MovementReaderDto>> {
		val visibility = currentUser.visibilityFor(isVisible, projectId, REGISTRY_PROJECT_MOVEMENT_D)
		val pageable = PageableModel(page * size, size)
		val searchParams = MovementSearchParamModel(
			visibility,
			hasActivity = null,
			type,
			from,
			to,
			sort = sort.toSortModel(MovementSearchParamModel.SORT_FIELDS),
		)

		return service.findActivityMovementsPage(projectId, id, pageable, searchParams)
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

	private fun parseDuration(duration: String?): Duration? {
		val parsed = try {
			duration?.let(Duration::parse)
		} catch (e: IllegalArgumentException) {
			throw RegistryException(BAD_REQUEST, ACTIVITY_DURATION_FORMAT_FAILED)
		}

		if (parsed != null && (!parsed.isFinite() || parsed.isNegative() || parsed > MAX_FILTER_DURATION)) {
			throw RegistryException(BAD_REQUEST, ACTIVITY_DURATION_FORMAT_FAILED)
		}
		return parsed
	}

	private companion object {
		val MAX_FILTER_DURATION: Duration = 366.days
	}
}
