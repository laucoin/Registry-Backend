package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.IActivityService
import fr.laucoin.registry.backend.infrastructure.out.api.controller.IActivityV1Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ActivityReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ActivityWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.ActivityReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.ActivityWriterDtoMapper
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class ActivityV1Controller(
	private val service: IActivityService,
	private val readerMapper: ActivityReaderDtoMapper,
	private val movementReaderMapper: MovementReaderDtoMapper,
	private val writerMapper: ActivityWriterDtoMapper,
): IActivityV1Controller {
	override fun findActivities(
		projectId: UUID,
		pageNumber: Int,
		pageSize: Int,
		textSearched: String?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
	): Mono<PageModel<ActivityReaderDto>> {
		val pageable = PageableModel(pageNumber * pageSize, pageSize)
		val searchParams = ActivitySearchParamModel(
			textSearched, visibilitySearched, availabilitySearched, dateTimeSearched
		)

		return service.findActivitiesPage(projectId, pageable, searchParams).map(readerMapper::toDtoPage)
	}

	override fun findActivityById(projectId: UUID, id: UUID): Mono<ActivityReaderDto> {
		return service.findActivityById(projectId, id, visibilitySearched = null).map(readerMapper::toDto)
	}

	override fun findActivityMovements(
		projectId: UUID,
		id: UUID,
		pageNumber: Int,
		pageSize: Int,
		visibilitySearched: Boolean?,
		typeSearched: MovementTypeEnum?,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?,
	): Mono<PageModel<MovementReaderDto>> {
		val pageable = PageableModel(pageNumber * pageSize, pageSize)
		val searchParams = MovementSearchParamModel(
			visibilitySearched,
			linkedToActivity = null,
			typeSearched,
			startDateTimeSearched,
			endDateTimeSearched
		)

		return service.findActivityMovementsPage(projectId, id, pageable, searchParams)
			.map(movementReaderMapper::toDtoPage)
	}

	override fun createActivity(
		currentUser: CurrentUserModel,
		projectId: UUID,
		activity: ActivityWriterDto
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
