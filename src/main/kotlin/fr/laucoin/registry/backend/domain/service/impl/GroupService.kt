package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_LAST_MEMBERS_CANNOT_BE_REMOVED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_ALREADY_ADDED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_NOT_FOUND_IN_GROUP_EVENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.repository.IGroupModelRepository
import fr.laucoin.registry.backend.domain.repository.IParticipantModelRepository
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IEventService
import fr.laucoin.registry.backend.domain.service.IGroupService
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class GroupService(
    private val eventService: IEventService,
    private val repository: IGroupModelRepository,
    private val participantRepository: IParticipantModelRepository,
    @Value("\${registry.feature.group.searched.max-participant-result}")
    private val maxParticipantResult: Int,
): IGroupService, GenericService() {
    override fun findGroupsPage(
        eventId: UUID,
        pageable: PageableModel,
        searchParams: GroupSearchParamModel,
    ): Mono<PageModel<GroupModel>> {
        return repository.findPage(eventId, pageable, searchParams)
    }

    override fun findGroupsMembers(eventId: UUID, groupIds: List<UUID>): Flux<Pair<UUID, List<ParticipantModel>>> {
        return repository.findContent(eventId, groupIds)
    }

    override fun findGroupMembersPageByGroupId(
        eventId: UUID,
        id: UUID,
        pageable: PageableModel,
        searchParams: ParticipantSearchParamModel,
    ): Mono<PageModel<ParticipantModel>> {
        return participantRepository.findPageByGroupId(
            eventId,
            id,
            pageable,
            searchParams,
        )
    }

    override fun findGroupById(eventId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<GroupModel> {
        return repository.findById(eventId, id, visibilitySearched)
            .notFoundIfEmpty(id)
    }

    override fun searchParticipants(eventId: UUID, textSearched: String?): Flux<ParticipantModel> {
        return participantRepository.findWithLimit(
            maxParticipantResult,
            eventId,
            ParticipantSearchParamModel(textSearched, visibilitySearched = true)
        )
    }

    override fun createGroup(currentUser: CurrentUserModel, group: GroupModel): Mono<GroupModel> {
        return eventService.validateDateTimes(
            group.event !!.id !!,
            group.startAvailability,
            group.endAvailability,
            GROUP_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE,
        )
            .flatMap { validateMembers(group.event !!.id !!, group, group.members.mapNotNull { p -> p.id }) }
            .flatMap { repository.create(group.apply { create(currentUser) }) }
    }

    override fun updateGroupById(currentUser: CurrentUserModel, eventId: UUID, id: UUID, group: GroupModel): Mono<GroupModel> {
        return eventService.validateDateTimes(
            group.event !!.id !!,
            group.startAvailability,
            group.endAvailability,
            GROUP_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE,
        )
            .flatMap { findGroupById(eventId, id, visibilitySearched = null) }
            .flatMap {
                val newMemberIds: List<UUID> = it.getNewMemberIds(group)
                if (newMemberIds.isEmpty()) Mono.just(it)
                else validateMembers(eventId, it, newMemberIds)
            }
            .map {
                it.apply {
                    it.name = group.name
                    it.startAvailability = group.startAvailability
                    it.endAvailability = group.endAvailability
                    it.members = group.members
                }
            }
            .updateGroup(currentUser)
    }

    override fun addMembersToGroupById(
        currentUser: CurrentUserModel,
        eventId: UUID,
        id: UUID,
        memberIds: List<UUID>
    ): Mono<Pair<List<UUID>, List<UUID>>> {
        return findGroupById(eventId, id, visibilitySearched = null)
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

    override fun removeMemberFromGroupById(currentUser: CurrentUserModel, eventId: UUID, id: UUID, memberId: UUID): Mono<GroupModel> {
        return findGroupById(eventId, id, visibilitySearched = null)
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

    private fun Mono<GroupModel>.updateGroup(currentUser: CurrentUserModel) = flatMap {
        repository.update(it.apply { update(currentUser) })
    }

    private fun validateMembers(eventId: UUID, group: GroupModel, newMemberIds: List<UUID>): Mono<GroupModel> {
        return participantRepository.findAllByIds(eventId, newMemberIds, visibilitySearched = null)
            .collectList()
            .handle { it, handle ->
                when {
                    it.size != newMemberIds.size -> handle.error(
                        RegistryException(
                            NOT_FOUND,
                            GROUP_MEMBERS_NOT_FOUND_IN_GROUP_EVENT,
                        )
                    )

                    it.any(ParticipantModel::isNotUsable) -> handle.error(
                        RegistryException(
                            CONFLICT,
                            GROUP_MEMBERS_NOT_VISIBLE,
                        )
                    )

                    else -> handle.next(group)
                }
            }
    }

    override fun disableGroupById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<GroupModel> {
        return findGroupById(eventId, id, visibilitySearched = true)
            .updateVisibility(visibility = false)
            .updateGroup(currentUser)
    }

    override fun enableGroupById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<GroupModel> {
        return findGroupById(eventId, id, visibilitySearched = false)
            .updateVisibility(visibility = true)
            .updateGroup(currentUser)
    }

    override fun deleteGroupById(eventId: UUID, id: UUID): Mono<Void> {
        return findGroupById(eventId, id, visibilitySearched = null)
            .flatMap { repository.deleteById(id) }
    }
}
