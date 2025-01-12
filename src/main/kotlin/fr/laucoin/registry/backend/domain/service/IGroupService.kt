package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.UserModel
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IGroupService {
    fun findGroups(
        eventId: UUID,
        order: Direction,
        onlyVisible: Boolean,
        onlyPresent: Boolean,
        searched: String?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?,
    ): Flux<GroupModel>

    fun findGroupMembersByGroupId(
        eventId: UUID,
        id: UUID,
        order: Direction,
        onlyVisible: Boolean,
        onlyPresent: Boolean,
        searched: String?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?,
    ): Flux<ParticipantModel>

    fun findGroupById(eventId: UUID, id: UUID, onlyVisible: Boolean): Mono<GroupModel>
    fun searchParticipants(eventId: UUID, searched: String?): Flux<ParticipantModel>
    fun createGroup(currentUser: UserModel, group: GroupModel): Mono<GroupModel>
    fun updateGroupById(currentUser: UserModel, eventId: UUID, id: UUID, group: GroupModel): Mono<GroupModel>
    fun addMembersToGroupById(currentUser: UserModel, eventId: UUID, id: UUID, memberIds: List<UUID>): Mono<List<UUID>>
    fun removeMemberFromGroupById(currentUser: UserModel, eventId: UUID, id: UUID, memberId: UUID): Mono<GroupModel>
    fun disableGroupById(currentUser: UserModel, eventId: UUID, id: UUID): Mono<GroupModel>
    fun enableGroupById(currentUser: UserModel, eventId: UUID, id: UUID): Mono<GroupModel>
    fun deleteGroupById(eventId: UUID, id: UUID): Mono<Void>
}
