package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.service.IGroupService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IGroupController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto.Companion.paginate
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.AddedGroupMembersReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.GroupReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.GroupWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.AddedGroupMembersReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.GroupReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.ParticipantReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.GroupWriterDtoMapper
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort.Direction
import org.springframework.http.HttpStatus.MULTI_STATUS
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class GroupController(
    private val service: IGroupService,
    private val readerMapper: GroupReaderDtoMapper,
    private val participantReaderMapper: ParticipantReaderDtoMapper,
    private val addedGroupMembersReaderMapper: AddedGroupMembersReaderDtoMapper,
    private val writerMapper: GroupWriterDtoMapper,
    @Value("\${registry.feature.group.searched.max-participant-result}")
    private val maxParticipantResult: Long,
): IGroupController {
    override fun findGroups(
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
    ): Mono<PageDto<GroupReaderDto>> {
        return service.findGroups(eventId, order, onlyVisible, onlyPresent, searched, startDateTime, endDateTime)
            .paginate(offset, limit)
            .map { readerMapper.toDtoPage(it, locale) }
    }

    override fun findGroupMembersByGroupId(
        locale: Locale,
        eventId: UUID,
        id: UUID,
        offset: Int,
        limit: Int,
        order: Direction,
        onlyVisible: Boolean,
        onlyPresent: Boolean,
        searched: String?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ): Mono<PageDto<ParticipantReaderDto>> {
        return service.findGroupMembersByGroupId(eventId, id, order, onlyVisible, onlyPresent, searched, startDateTime, endDateTime)
            .paginate(offset, limit)
            .map { participantReaderMapper.toDtoPage(it, locale) }
    }

    override fun findGroupById(locale: Locale, eventId: UUID, id: UUID): Mono<GroupReaderDto> {
        return service.findGroupById(eventId, id, onlyVisible = false)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun searchParticipants(locale: Locale, eventId: UUID, searched: String?): Flux<ParticipantReaderDto> {
        return service.searchParticipants(eventId, searched)
            .take(maxParticipantResult)
            .map { participantReaderMapper.toDto(it, locale) }
    }

    override fun createGroup(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        group: GroupWriterDto,
    ): Mono<GroupReaderDto> {
        return service.createGroup(currentUser, writerMapper.toModel(group, eventId))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun updateGroupById(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        id: UUID,
        group: GroupWriterDto,
    ): Mono<GroupReaderDto> {
        return service.updateGroupById(currentUser, eventId, id, writerMapper.toModel(group, eventId))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun addMembersToGroupById(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        id: UUID,
        memberIds: List<UUID>,
    ): Mono<ResponseEntity<AddedGroupMembersReaderDto>> {
        return service.addMembersToGroupById(currentUser, eventId, id, memberIds)
            .map { addedGroupMembersReaderMapper.toDto(it, locale) }
            .map { ResponseEntity.status(if (it.notAddedMemberIds.isEmpty()) OK else MULTI_STATUS).body(it) }
    }

    override fun removeMemberFromGroupById(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        id: UUID,
        memberId: UUID
    ): Mono<GroupReaderDto> {
        return service.removeMemberFromGroupById(currentUser, eventId, id, memberId)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun disableGroupById(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        id: UUID
    ): Mono<GroupReaderDto> {
        return service.disableGroupById(currentUser, eventId, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun enableGroupById(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        id: UUID
    ): Mono<GroupReaderDto> {
        return service.enableGroupById(currentUser, eventId, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun deleteGroupById(eventId: UUID, id: UUID): Mono<Void> {
        return service.deleteGroupById(eventId, id)
    }
}
