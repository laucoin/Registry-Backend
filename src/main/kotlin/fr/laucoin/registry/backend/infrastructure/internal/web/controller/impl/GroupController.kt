package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.service.IGroupService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IGroupController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto.Companion.paginate
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.AddedGroupMembersReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.GroupReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.GroupWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.GroupReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.ParticipantReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.GroupWriterDtoMapper
import java.time.ZonedDateTime
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
    private val writerMapper: GroupWriterDtoMapper,
    @Value("\${registry.feature.group.searched.max-participant-result}")
    private val maxParticipantResult: Long,
): IGroupController {
    override fun findGroups(
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
            .map(readerMapper::toDto)
            .paginate(offset, limit)
    }

    override fun findGroupMembersByGroupId(
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
            .map(participantReaderMapper::toDto)
            .paginate(offset, limit)
    }

    override fun findGroupById(eventId: UUID, id: UUID): Mono<GroupReaderDto> {
        return service.findGroupById(eventId, id, onlyVisible = false)
            .map(readerMapper::toDto)
    }

    override fun searchParticipants(eventId: UUID, searched: String?): Flux<ParticipantReaderDto> {
        return service.searchParticipants(eventId, searched)
            .take(maxParticipantResult)
            .map(participantReaderMapper::toDto)
    }

    override fun createGroup(currentUser: CurrentUserModel, eventId: UUID, group: GroupWriterDto): Mono<GroupModel> {
        return service.createGroup(currentUser, writerMapper.toModel(group, eventId))
    }

    override fun updateGroupById(
        currentUser: CurrentUserModel,
        eventId: UUID,
        id: UUID,
        group: GroupWriterDto,
    ): Mono<GroupModel> {
        return service.updateGroupById(currentUser, eventId, id, writerMapper.toModel(group, eventId))
    }

    override fun addMembersToGroupById(
        currentUser: CurrentUserModel,
        eventId: UUID,
        id: UUID,
        memberIds: List<UUID>,
    ): Mono<ResponseEntity<AddedGroupMembersReaderDto>> {
        return service.addMembersToGroupById(currentUser, eventId, id, memberIds)
            .map { AddedGroupMembersReaderDto(members = it) }
            .map { body ->
                if (body.members.size == memberIds.size) {
                    ResponseEntity.status(OK).body(body)
                } else {
                    body.notAddedMemberIds = memberIds.filter { ! body.members.contains(it) }
                    ResponseEntity.status(MULTI_STATUS).body(body)
                }
            }
    }

    override fun removeMemberFromGroupById(currentUser: CurrentUserModel, eventId: UUID, id: UUID, memberId: UUID): Mono<GroupModel> {
        return service.removeMemberFromGroupById(currentUser, eventId, id, memberId)
    }

    override fun disableGroupById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<GroupModel> {
        return service.disableGroupById(currentUser, eventId, id)
    }

    override fun enableGroupById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<GroupModel> {
        return service.enableGroupById(currentUser, eventId, id)
    }

    override fun deleteGroupById(eventId: UUID, id: UUID): Mono<Void> {
        return service.deleteGroupById(eventId, id)
    }
}
