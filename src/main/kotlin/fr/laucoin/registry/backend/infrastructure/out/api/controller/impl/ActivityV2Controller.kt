package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.ActivitySortFieldEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.service.IActivityService
import fr.laucoin.registry.backend.infrastructure.out.api.controller.IActivityV2Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ActivityReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ActivityWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.SortedPageQueryDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.PageQueryDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.PageQueryDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.ActivityReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.ActivityWriterDtoMapper
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.UUID

@RestController
class ActivityV2Controller(
	private val service: IActivityService,
	private val readerMapper: ActivityReaderDtoMapper,
	private val movementReaderMapper: MovementReaderDtoMapper,
	private val writerMapper: ActivityWriterDtoMapper,
) : IActivityV2Controller {
	override fun findActivities(
		projectId: UUID,
		pageQuery: SortedPageQueryDto,
		q: String?,
		visible: Boolean?,
		available: Boolean?,
		dateTime: ZonedDateTime?,
	): Mono<PageModel<ActivityReaderDto>> {
		val pageable = PageQueryDtoMapper.toPageable(pageQuery)
		val searchParams = ActivitySearchParamModel(q, visible, available, dateTime)
		val sortModels = PageQueryDtoMapper.toSortModels(pageQuery, ActivitySortFieldEnum::fromParamName)

		return service.findActivitiesPage(projectId, pageable, searchParams, sortModels).map(readerMapper::toDtoPage)
	}

	override fun findActivityById(projectId: UUID, id: UUID): Mono<ActivityReaderDto> {
		return service.findActivityById(projectId, id, visibilitySearched = null).map(readerMapper::toDto)
	}

	override fun findActivityMovements(
		projectId: UUID,
		id: UUID,
		pageQuery: PageQueryDto,
		visible: Boolean?,
		type: MovementTypeEnum?,
		startDateTime: ZonedDateTime?,
		endDateTime: ZonedDateTime?,
	): Mono<PageModel<MovementReaderDto>> {
		val pageable = PageQueryDtoMapper.toPageable(pageQuery)
		val searchParams = MovementSearchParamModel(
			visible,
			linkedToActivity = null,
			type,
			startDateTime,
			endDateTime
		)

		return service.findActivityMovementsPage(projectId, id, pageable, searchParams)
			.map(movementReaderMapper::toDtoPage)
	}

	override fun createActivity(
		currentUser: CurrentUserModel,
		projectId: UUID,
		activity: ActivityWriterDto,
	): Mono<ActivityReaderDto> {
		val activityModel = writerMapper.toModel(activity, projectId)
		return service.createActivity(currentUser, activityModel).map(readerMapper::toDto)
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
