package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ApiConst.API_V2
import fr.laucoin.registry.backend.domain.enumeration.ActivitySortFieldEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.service.IActivityService
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IActivityV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.PageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.SortedPageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ActivityReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.OngoingActivityOutingReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PageReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ActivityWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.PageQueryDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.SortParamDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ActivityReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.OngoingActivityOutingReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.PageReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.ActivityWriterDtoMapper
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class ActivityV2Controller(
	private val service: IActivityService,
	private val readerMapper: ActivityReaderDtoMapper,
	private val movementReaderMapper: MovementReaderDtoMapper,
	private val ongoingOutingReaderMapper: OngoingActivityOutingReaderDtoMapper,
	private val writerMapper: ActivityWriterDtoMapper,
	private val pageQueryMapper: PageQueryDtoMapper,
	private val sortParamMapper: SortParamDtoMapper,
	private val pageReaderMapper: PageReaderDtoMapper,
): IActivityV2Controller {
	override fun findActivities(
		projectId: UUID,
		page: SortedPageQueryDto,
		q: String?,
		visible: Boolean?,
		available: Boolean?,
		dateTime: ZonedDateTime?,
	): Mono<PageReaderDto<ActivityReaderDto>> {
		val pageable = pageQueryMapper.toPageable(page)
		val sortFields = sortParamMapper.toSortModels(page.sort, page.direction) { key ->
			ActivitySortFieldEnum.entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
		}
		val searchParams = ActivitySearchParamModel(q, visible, available, dateTime)

		return service.findActivitiesPage(projectId, pageable, searchParams, sortFields)
			.map { pageReaderMapper.toDto(it, readerMapper::toDto) }
	}

	override fun findActivityById(projectId: UUID, id: UUID): Mono<ActivityReaderDto> {
		return service.findActivityById(projectId, id, visibilitySearched = null).map(readerMapper::toDto)
	}

	override fun findActivityMovements(
		projectId: UUID,
		id: UUID,
		page: PageQueryDto,
		visible: Boolean?,
		type: MovementTypeEnum?,
		startDateTime: ZonedDateTime?,
		endDateTime: ZonedDateTime?,
	): Mono<PageReaderDto<MovementReaderDto>> {
		val pageable = pageQueryMapper.toPageable(page)
		val searchParams = MovementSearchParamModel(visible, linkedToActivity = null, type, startDateTime, endDateTime)

		return service.findActivityMovementsPage(projectId, id, pageable, searchParams)
			.map { pageReaderMapper.toDto(it, movementReaderMapper::toDto) }
	}

	override fun findOngoingActivityOutings(projectId: UUID, limit: Int): Flux<OngoingActivityOutingReaderDto> {
		return service.findOngoingActivityOutings(projectId, limit).map(ongoingOutingReaderMapper::toDto)
	}

	override fun createActivity(
		currentUser: CurrentUserModel,
		projectId: UUID,
		activity: ActivityWriterDto,
	): Mono<ResponseEntity<ActivityReaderDto>> {
		val activityModel = writerMapper.toModel(activity, projectId)
		return service.createActivity(currentUser, activityModel).map(readerMapper::toDto).map {
			ResponseEntity.created(URI.create("$API_V2/projects/$projectId/activities/${it.id}")).body(it)
		}
	}

	override fun updateActivityById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		activity: ActivityWriterDto,
	): Mono<ActivityReaderDto> {
		val activityModel = writerMapper.toModel(activity, projectId)
		return service.updateActivityById(currentUser, projectId, id, activityModel).map(readerMapper::toDto)
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
