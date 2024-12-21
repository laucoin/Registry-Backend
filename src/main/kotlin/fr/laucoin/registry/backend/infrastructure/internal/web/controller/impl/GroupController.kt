package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.extension.ReactiveExt.currentUser
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageModel.Companion.paginate
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.service.IGroupService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IGroupController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.AddedGroupMembersDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.GroupDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.GroupDtoMapper
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
    private val mapper: GroupDtoMapper,
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
    ): Mono<PageModel<GroupModel>> {
        return service.findGroups(eventId, order, onlyVisible, onlyPresent, searched, startDateTime, endDateTime)
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
    ): Mono<PageModel<ParticipantModel>> {
        return service.findGroupMembersByGroupId(eventId, id, order, onlyVisible, onlyPresent, searched, startDateTime, endDateTime)
            .paginate(offset, limit)
    }

    override fun findGroupById(eventId: UUID, id: UUID): Mono<GroupModel> {
        return service.findGroupById(eventId, id, onlyVisible = false)
    }

    override fun searchParticipants(eventId: UUID, searched: String?): Flux<ParticipantModel> {
        return service.searchParticipants(eventId, searched)
            .take(maxParticipantResult)
    }

    override fun createGroup(eventId: UUID, group: GroupDto): Mono<GroupModel> {
        return currentUser().flatMap { service.createGroup(it, mapper.toModel(group, eventId)) }
    }

    override fun updateGroupById(eventId: UUID, id: UUID, group: GroupDto): Mono<GroupModel> {
        return currentUser().flatMap { service.updateGroupById(it, eventId, id, mapper.toModel(group, eventId)) }
    }

    override fun addMembersToGroupById(eventId: UUID, id: UUID, memberIds: List<UUID>): Mono<ResponseEntity<AddedGroupMembersDto>> {
        return currentUser().flatMap { service.addMembersToGroupById(it, eventId, id, memberIds) }
            .map { AddedGroupMembersDto(members = it) }
            .map { body ->
                if (body.members.size == memberIds.size) {
                    ResponseEntity.status(OK).body(body)
                } else {
                    body.notAddedMemberIds = memberIds.filter { ! body.members.contains(it) }
                    ResponseEntity.status(MULTI_STATUS).body(body)
                }
            }
    }

    override fun removeMemberFromGroupById(eventId: UUID, id: UUID, memberId: UUID): Mono<GroupModel> {
        return currentUser().flatMap { service.removeMemberFromGroupById(it, eventId, id, memberId) }
    }

    override fun disableGroupById(eventId: UUID, id: UUID): Mono<GroupModel> {
        return currentUser().flatMap { service.disableGroupById(it, eventId, id) }
    }

    override fun enableGroupById(eventId: UUID, id: UUID): Mono<GroupModel> {
        return currentUser().flatMap { service.enableGroupById(it, eventId, id) }
    }

    override fun deleteGroupById(eventId: UUID, id: UUID): Mono<Void> {
        return service.deleteGroupById(eventId, id)
    }
}
