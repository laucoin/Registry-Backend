package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.service.IParticipantService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IParticipantController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto.Companion.paginate
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.GroupReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.ParticipantWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.GroupReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.PartialUserReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.ParticipantReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.ParticipantWriterDtoMapper
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort.Direction
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class ParticipantController(
    private val service: IParticipantService,
    private val groupReaderMapper: GroupReaderDtoMapper,
    private val readerMapper: ParticipantReaderDtoMapper,
    private val writerMapper: ParticipantWriterDtoMapper,
    private val userMapper: PartialUserReaderDtoMapper,
    @Value("\${registry.feature.participant.searched.max-user-result}")
    private val maxUserResult: Long,
    @Value("\${registry.feature.participant.searched.max-group-result}")
    private val maxGroupResult: Long,
): IParticipantController {
    override fun findParticipants(
        eventId: UUID,
        offset: Int,
        limit: Int,
        order: Direction,
        onlyVisible: Boolean,
        onlyPresent: Boolean,
        searched: String?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ): Mono<PageDto<ParticipantReaderDto>> {
        return service.findParticipantsByEventId(
            eventId,
            order,
            onlyVisible,
            onlyPresent,
            searched,
            startDateTime,
            endDateTime
        )
            .map(readerMapper::toDto)
            .paginate(offset, limit)
    }

    override fun findParticipantById(eventId: UUID, id: UUID): Mono<ParticipantReaderDto> {
        return service.findParticipantById(eventId, id, onlyVisible = false)
            .map(readerMapper::toDto)
    }

    override fun searchUsers(eventId: UUID, searched: String?): Flux<PartialUserReaderDto> {
        return service.searchUsers(eventId, searched)
            .take(maxUserResult)
            .map(userMapper::toDto)
    }

    override fun searchGroups(eventId: UUID, searched: String?): Flux<GroupReaderDto> {
        return service.searchGroups(eventId, searched)
            .take(maxGroupResult)
            .map(groupReaderMapper::toDto)
    }

    override fun createParticipant(
        currentUser: CurrentUserModel,
        eventId: UUID,
        participant: ParticipantWriterDto
    ): Mono<ParticipantModel> {
        return service.createParticipant(currentUser, writerMapper.toModel(participant, eventId))
    }

    override fun updateParticipantById(
        currentUser: CurrentUserModel,
        eventId: UUID,
        id: UUID,
        participant: ParticipantWriterDto,
    ): Mono<ParticipantModel> {
        return service.updateParticipantById(currentUser, eventId, id, writerMapper.toModel(participant, eventId))
    }

    override fun disableParticipantById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<ParticipantModel> {
        return service.disableParticipantById(currentUser, eventId, id)
    }

    override fun enableParticipantById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<ParticipantModel> {
        return service.enableParticipantById(currentUser, eventId, id)
    }

    override fun deleteParticipantById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<Void> {
        return service.deleteParticipantById(currentUser, eventId, id)
    }
}
