package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_LAST_MEMBERS_CANNOT_BE_REMOVED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_ALREADY_ADDED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_NOT_FOUND_IN_GROUP_EVENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.repository.IGroupModelRepository
import fr.laucoin.registry.backend.domain.repository.IParticipantModelRepository
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IEventService
import fr.laucoin.registry.backend.domain.service.IGroupService
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class GroupService(
    private val repository: IGroupModelRepository,
    private val participantRepository: IParticipantModelRepository,
    private val eventService: IEventService,
): IGroupService, GenericService() {
    override fun findGroups(
        eventId: UUID,
        order: Direction,
        onlyVisible: Boolean,
        onlyPresent: Boolean,
        searched: String?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ): Flux<GroupModel> {
        return repository.findAll(eventId, onlyVisible, onlyPresent, startDateTime, endDateTime)
            .searchAndSort(order, searched, compareBy { it.name })
    }

    override fun findGroupMembersByGroupId(
        eventId: UUID,
        id: UUID,
        order: Direction,
        onlyVisible: Boolean,
        onlyPresent: Boolean,
        searched: String?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ): Flux<ParticipantModel> {
        return participantRepository.findAll(
            eventId,
            onlyVisible,
            onlyPresent,
            startDateTime,
            endDateTime
        )
            .filter { it.groups.any { group -> group.id == id } }
            .searchAndSort(order, searched, compareBy { it.lastName })
    }

    override fun findGroupById(eventId: UUID, id: UUID, onlyVisible: Boolean): Mono<GroupModel> {
        return repository.findById(eventId, id, onlyVisible)
            .notFoundIfEmpty(id)
    }

    override fun searchParticipants(eventId: UUID, searched: String?): Flux<ParticipantModel> {
        return participantRepository.findAll(
            eventId,
            onlyVisible = true,
            onlyPresent = false,
            startDateTime = null,
            endDateTime = null
        ).searchAndSort(order = ASC, searched, compareBy { it.lastName })
    }

    override fun createGroup(currentUser: UserModel, group: GroupModel): Mono<GroupModel> {
        return eventService.validateDateTimes(
            group.event !!.id !!,
            group.begin,
            group.end,
            GROUP_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE,
        )
            .flatMap { validateMembers(group.event !!.id !!, group, group.members.mapNotNull { p -> p.id }) }
            .flatMap { repository.create(group.apply { create(currentUser) }) }
    }

    override fun updateGroupById(currentUser: UserModel, eventId: UUID, id: UUID, group: GroupModel): Mono<GroupModel> {
        return eventService.validateDateTimes(
            group.event !!.id !!,
            group.begin,
            group.end,
            GROUP_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE,
        )
            .flatMap { findGroupById(eventId, id, onlyVisible = false) }
            .flatMap {
                val newMemberIds: List<UUID> = it.getNewMemberIds(group)
                if (newMemberIds.isEmpty()) Mono.just(it)
                else validateMembers(eventId, it, newMemberIds)
            }
            .flatMap {
                it.let {
                    it.name = group.name
                    it.begin = group.begin
                    it.end = group.end
                    it.members = group.members
                    it.update(currentUser)
                }
                repository.update(it)
            }
    }

    override fun addMembersToGroupById(
        currentUser: UserModel,
        eventId: UUID,
        id: UUID,
        memberIds: List<UUID>
    ): Mono<Pair<List<UUID>, List<UUID>>> {
        return findGroupById(eventId, id, onlyVisible = false)
            .map { Pair(it, it.getNewMemberIds(memberIds)) }
            .handle { it, handle ->
                if (it.second.isEmpty()) {
                    handle.error(
                        RegistryException(
                            CONFLICT,
                            GROUP_MEMBERS_ALREADY_ADDED,
                        )
                    )
                } else {
                    handle.next(it)
                }
            }
            .flatMap { (group, newMemberIds) ->
                validateMembers(eventId, group, newMemberIds)
                    .map { _ -> newMemberIds.map { ParticipantModel().apply { this.id = it } } }
                    .map { group.apply { members = members.plus(it) } }
                    .updateGroup(currentUser)
                    .map { Pair(newMemberIds, memberIds.minus(newMemberIds.toSet())) }
            }
    }

    override fun removeMemberFromGroupById(currentUser: UserModel, eventId: UUID, id: UUID, memberId: UUID): Mono<GroupModel> {
        return findGroupById(eventId, id, onlyVisible = false)
            .map { it.apply { members = members.filter { m -> m.id != memberId } } }
            .handle { it, handle ->
                if (it.members.isEmpty()) {
                    handle.error(
                        RegistryException(
                            FORBIDDEN,
                            GROUP_LAST_MEMBERS_CANNOT_BE_REMOVED,
                        )
                    )
                } else {
                    handle.next(it)
                }
            }
            .updateGroup(currentUser)
    }

    private fun Mono<GroupModel>.updateGroup(currentUser: UserModel) = flatMap {
        repository.update(it.apply { update(currentUser) })
    }

    private fun validateMembers(eventId: UUID, group: GroupModel, newMemberIds: List<UUID>): Mono<GroupModel> {
        return participantRepository.findAllByIds(eventId, newMemberIds, onlyVisible = false)
            .collectList()
            .handle { it, handle ->
                when {
                    it.size != newMemberIds.size -> handle.error(
                        RegistryException(
                            NOT_FOUND,
                            GROUP_MEMBERS_NOT_FOUND_IN_GROUP_EVENT,
                        )
                    )

                    it.any { m -> m.isNotVisible() || m.purged == true } -> handle.error(
                        RegistryException(
                            CONFLICT,
                            GROUP_MEMBERS_NOT_VISIBLE,
                        )
                    )

                    else -> handle.next(group)
                }
            }
    }

    override fun disableGroupById(currentUser: UserModel, eventId: UUID, id: UUID): Mono<GroupModel> {
        return findGroupById(eventId, id, onlyVisible = true)
            .updateVisibility(visibility = false)
            .updateGroup(currentUser)
    }

    override fun enableGroupById(currentUser: UserModel, eventId: UUID, id: UUID): Mono<GroupModel> {
        return findGroupById(eventId, id, onlyVisible = false)
            .updateVisibility(visibility = true)
            .updateGroup(currentUser)
    }

    override fun deleteGroupById(eventId: UUID, id: UUID): Mono<Void> {
        return findGroupById(eventId, id, onlyVisible = false)
            .flatMap { repository.deleteById(id) }
    }
}
