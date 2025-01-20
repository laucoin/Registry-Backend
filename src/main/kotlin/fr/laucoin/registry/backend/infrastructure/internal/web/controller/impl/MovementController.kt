package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementParticipantsAndGroupsModel
import fr.laucoin.registry.backend.domain.service.IMovementService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IMovementController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto.Companion.paginate
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.MovementWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.MovementWriterDtoMapper
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort.Direction
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class MovementController(
    private val service: IMovementService,
    private val readerMapper: MovementReaderDtoMapper,
    private val writerMapper: MovementWriterDtoMapper,
    @Value("\${registry.feature.movement.searched.max-participant-result}")
    private val maxParticipantResult: Int,
    @Value("\${registry.feature.movement.searched.max-group-result}")
    private val maxGroupResult: Int,
): IMovementController {
    override fun findMovements(
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
            .map(readerMapper::toDto)
            .paginate(offset, limit)
    }

    override fun findMovementById(eventId: UUID, id: UUID): Mono<MovementReaderDto> {
        return service.findMovementById(eventId, id, onlyVisible = false)
            .map(readerMapper::toDto)
    }

    override fun searchParticipantsAndGroups(eventId: UUID, searched: String?): Mono<MovementParticipantsAndGroupsModel> {
        return service.searchParticipantsAndGroups(eventId, searched)
            .map {
                MovementParticipantsAndGroupsModel(
                    participants = it.participants.take(maxParticipantResult),
                    groups = it.groups.take(maxGroupResult)
                )
            }
    }

    override fun createMovement(currentUser: CurrentUserModel, eventId: UUID, movement: MovementWriterDto): Mono<MovementModel> {
        return service.createMovement(currentUser, writerMapper.toModel(movement, eventId))
    }

    override fun updateMovementById(
        currentUser: CurrentUserModel,
        eventId: UUID,
        id: UUID,
        movement: MovementWriterDto
    ): Mono<MovementModel> {
        return service.updateMovementById(currentUser, eventId, id, writerMapper.toModel(movement, eventId))
    }

    override fun disableMovementById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<MovementModel> {
        return service.disableMovementById(currentUser, eventId, id)
    }

    override fun enableMovementById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<MovementModel> {
        return service.enableMovementById(currentUser, eventId, id)
    }

    override fun deleteMovementById(eventId: UUID, id: UUID): Mono<Void> {
        return service.deleteMovementById(eventId, id)
    }
}
