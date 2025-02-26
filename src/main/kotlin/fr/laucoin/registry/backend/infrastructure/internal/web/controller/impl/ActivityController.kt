package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.IActivityService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IActivityController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ActivityReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.ActivityWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.ActivityReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.ActivityWriterDtoMapper
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
        eventId: UUID,
        pageNumber: Int,
        pageSize: Int,
        textSearched: String?,
        visibilitySearched: Boolean?,
        availabilitySearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
    ): Mono<PageModel<ActivityReaderDto>> {
        return service.findActivitiesPage(
            eventId,
            PageableModel(pageNumber * pageSize, pageSize),
            ActivitySearchParamModel(textSearched, visibilitySearched, availabilitySearched, dateTimeSearched),
        ).map { readerMapper.toDtoPage(it, locale) }
    }

    override fun findActivityById(locale: Locale, eventId: UUID, id: UUID): Mono<ActivityReaderDto> {
        return service.findActivityById(eventId, id, visibilitySearched = null)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun findActivityMovements(
        locale: Locale,
        eventId: UUID,
        id: UUID,
        pageNumber: Int,
        pageSize: Int,
        visibilitySearched: Boolean?,
        typeSearched: MovementTypeEnum?,
        startDateTimeSearched: ZonedDateTime?,
        endDateTimeSearched: ZonedDateTime?,
    ): Mono<PageModel<MovementReaderDto>> {
        return service.findActivityMovementsPage(
            eventId,
            id,
            PageableModel(pageNumber * pageSize, pageSize),
            MovementSearchParamModel(visibilitySearched, typeSearched, startDateTimeSearched, endDateTimeSearched),
        ).map { movementReaderMapper.toDtoPage(it, locale) }
    }

    override fun createActivity(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        activity: ActivityWriterDto
    ): Mono<ActivityReaderDto> {
        return service.createActivity(currentUser, writerMapper.toModel(activity, eventId))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun updateActivityById(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        id: UUID,
        activity: ActivityWriterDto,
    ): Mono<ActivityReaderDto> {
        return service.updateActivityById(currentUser, eventId, id, writerMapper.toModel(activity, eventId))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun disableActivityById(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        id: UUID,
    ): Mono<ActivityReaderDto> {
        return service.disableActivityById(currentUser, eventId, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun enableActivityById(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        id: UUID,
    ): Mono<ActivityReaderDto> {
        return service.enableActivityById(currentUser, eventId, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun deleteActivityById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<Void> {
        return service.deleteActivityById(currentUser, eventId, id)
    }
}
