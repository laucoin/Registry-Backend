package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.service.IParticipantService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IParticipantController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto.Companion.paginate
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.GroupWithoutMemberReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.ParticipantWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.GroupWithoutMemberReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.PartialUserReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.ParticipantReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.ParticipantWriterDtoMapper
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort.Direction
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class ParticipantController(
    private val service: IParticipantService,
    private val readerMapper: ParticipantReaderDtoMapper,
    private val groupReaderMapper: GroupWithoutMemberReaderDtoMapper,
    private val partialUserReaderMapper: PartialUserReaderDtoMapper,
    private val writerMapper: ParticipantWriterDtoMapper,
    @Value("\${registry.feature.participant.searched.max-user-result}")
    private val maxUserResult: Long,
    @Value("\${registry.feature.participant.searched.max-group-result}")
    private val maxGroupResult: Long,
): IParticipantController {
    override fun findParticipants(
        locale: Locale,
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
            .paginate(offset, limit)
            .map { readerMapper.toDtoPage(it, locale) }
    }

    override fun findParticipantById(locale: Locale, eventId: UUID, id: UUID): Mono<ParticipantReaderDto> {
        return service.findParticipantById(eventId, id, onlyVisible = false)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun searchUsers(locale: Locale, eventId: UUID, searched: String?): Flux<PartialUserReaderDto> {
        return service.searchUsers(eventId, searched)
            .take(maxUserResult)
            .map { partialUserReaderMapper.toDto(it, locale) }
    }

    override fun searchGroups(locale: Locale, eventId: UUID, searched: String?): Flux<GroupWithoutMemberReaderDto> {
        return service.searchGroups(eventId, searched)
            .take(maxGroupResult)
            .map { groupReaderMapper.toDto(it, locale) }
    }

    override fun createParticipant(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        participant: ParticipantWriterDto
    ): Mono<ParticipantReaderDto> {
        return service.createParticipant(currentUser, writerMapper.toModel(participant, eventId))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun updateParticipantById(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        id: UUID,
        participant: ParticipantWriterDto,
    ): Mono<ParticipantReaderDto> {
        return service.updateParticipantById(currentUser, eventId, id, writerMapper.toModel(participant, eventId))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun disableParticipantById(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        id: UUID,
    ): Mono<ParticipantReaderDto> {
        return service.disableParticipantById(currentUser, eventId, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun enableParticipantById(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        id: UUID,
    ): Mono<ParticipantReaderDto> {
        return service.enableParticipantById(currentUser, eventId, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun deleteParticipantById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<Void> {
        return service.deleteParticipantById(currentUser, eventId, id)
    }
}
