package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.IActivityService
import fr.laucoin.registry.backend.infrastructure.out.api.controller.IActivityController
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ActivityReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ActivityWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.ActivityReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.ActivityWriterDtoMapper
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class ActivityController(
	private val service: IActivityService,
	private val readerMapper: ActivityReaderDtoMapper,
	private val movementReaderMapper: MovementReaderDtoMapper,
	private val writerMapper: ActivityWriterDtoMapper,
): IActivityController {
	override fun findActivities(
		locale: Locale,
		projectId: UUID,
		pageNumber: Int,
		pageSize: Int,
		textSearched: String?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
	): Mono<PageModel<ActivityReaderDto>> {
		return service.findActivitiesPage(
			projectId,
			PageableModel(pageNumber * pageSize, pageSize),
			ActivitySearchParamModel(textSearched, visibilitySearched, availabilitySearched, dateTimeSearched),
		).map { readerMapper.toDtoPage(it, locale) }
	}

	override fun findActivityById(locale: Locale, projectId: UUID, id: UUID): Mono<ActivityReaderDto> {
		return service.findActivityById(projectId, id, visibilitySearched = null)
			.map { readerMapper.toDto(it, locale) }
	}

	override fun findActivityMovements(
		locale: Locale,
		projectId: UUID,
		id: UUID,
		pageNumber: Int,
		pageSize: Int,
		visibilitySearched: Boolean?,
		typeSearched: MovementTypeEnum?,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?,
	): Mono<PageModel<MovementReaderDto>> {
		return service.findActivityMovementsPage(
			projectId,
			id,
			PageableModel(pageNumber * pageSize, pageSize),
			MovementSearchParamModel(
				visibilitySearched,
				linkedToActivity = null,
				typeSearched,
				startDateTimeSearched,
				endDateTimeSearched
			),
		).map { movementReaderMapper.toDtoPage(it, locale) }
	}

	override fun createActivity(
		currentUser: CurrentUserModel,
		locale: Locale,
		projectId: UUID,
		activity: ActivityWriterDto
	): Mono<ActivityReaderDto> {
		return service.createActivity(currentUser, writerMapper.toModel(activity, projectId))
			.map { readerMapper.toDto(it, locale) }
	}

	override fun updateActivityById(
		currentUser: CurrentUserModel,
		locale: Locale,
		projectId: UUID,
		id: UUID,
		activity: ActivityWriterDto,
	): Mono<ActivityReaderDto> {
		return service.updateActivityById(currentUser, projectId, id, writerMapper.toModel(activity, projectId))
			.map { readerMapper.toDto(it, locale) }
	}

	override fun disableActivityById(
		currentUser: CurrentUserModel,
		locale: Locale,
		projectId: UUID,
		id: UUID,
	): Mono<ActivityReaderDto> {
		return service.disableActivityById(currentUser, projectId, id)
			.map { readerMapper.toDto(it, locale) }
	}

	override fun enableActivityById(
		currentUser: CurrentUserModel,
		locale: Locale,
		projectId: UUID,
		id: UUID,
	): Mono<ActivityReaderDto> {
		return service.enableActivityById(currentUser, projectId, id)
			.map { readerMapper.toDto(it, locale) }
	}

	override fun deleteActivityById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Void> {
		return service.deleteActivityById(currentUser, projectId, id)
	}
}
