package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.service.IMovementService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IMovementController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto.Companion.paginate
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementParticipantsAndGroupsReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.MovementWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.MovementParticipantsAndGroupsReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.MovementTypeReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.MovementWriterDtoMapper
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort.Direction
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class MovementController(
    private val service: IMovementService,
    private val readerMapper: MovementReaderDtoMapper,
    private val movementTypeReaderMapper: MovementTypeReaderDtoMapper,
    private val movementParticipantsAndGroupsMapper: MovementParticipantsAndGroupsReaderDtoMapper,
    private val writerMapper: MovementWriterDtoMapper,
    @Value("\${registry.feature.movement.searched.max-participant-result}")
    private val maxParticipantResult: Int,
    @Value("\${registry.feature.movement.searched.max-group-result}")
    private val maxGroupResult: Int,
): IMovementController {
    override fun findMovements(
        locale: Locale,
        eventId: UUID,
        offset: Int,
        limit: Int,
        order: Direction,
        onlyVisible: Boolean,
        searched: String?,
        type: MovementTypeEnum?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ): Mono<PageDto<MovementReaderDto>> {
        return service.findMovements(eventId, order, onlyVisible, searched, type, startDateTime, endDateTime)
            .paginate(offset, limit)
            .map { readerMapper.toDtoPage(it, locale) }
    }

    override fun findMovementById(locale: Locale, eventId: UUID, id: UUID): Mono<MovementReaderDto> {
        return service.findMovementById(eventId, id, onlyVisible = false)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun searchParticipantsAndGroups(
        locale: Locale,
        eventId: UUID,
        searched: String?
    ): Mono<MovementParticipantsAndGroupsReaderDto> {
        return service.searchParticipantsAndGroups(eventId, searched)
            .map { Pair(it.t1.take(maxParticipantResult), it.t2.take(maxGroupResult)) }
            .map { movementParticipantsAndGroupsMapper.toDto(it, locale) }
    }

    override fun getAvailableMovementTypes(locale: Locale, eventId: UUID): Flux<LabelDto> {
        return service.availableMovementTypes()
            .map { movementTypeReaderMapper.toDto(it, locale) }
    }

    override fun createMovement(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        movement: MovementWriterDto,
    ): Mono<MovementReaderDto> {
        return service.createMovement(currentUser, writerMapper.toModel(movement, eventId))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun updateMovementById(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        id: UUID,
        movement: MovementWriterDto
    ): Mono<MovementReaderDto> {
        return service.updateMovementById(currentUser, eventId, id, writerMapper.toModel(movement, eventId))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun disableMovementById(currentUser: CurrentUserModel, locale: Locale, eventId: UUID, id: UUID): Mono<MovementReaderDto> {
        return service.disableMovementById(currentUser, eventId, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun enableMovementById(currentUser: CurrentUserModel, locale: Locale, eventId: UUID, id: UUID): Mono<MovementReaderDto> {
        return service.enableMovementById(currentUser, eventId, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun deleteMovementById(eventId: UUID, id: UUID): Mono<Void> {
        return service.deleteMovementById(eventId, id)
    }
}
