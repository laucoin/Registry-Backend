package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_DELETE_LAST_GROUP_MEMBER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_DISABLE_LAST_GROUP_MEMBER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_GROUPS_NOT_FOUND_IN_PARTICIPANT_EVENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_GROUPS_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_IN_EVENT_ALREADY_LINKED_TO_USER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.repository.IGroupModelRepository
import fr.laucoin.registry.backend.domain.repository.IParticipantModelRepository
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IEventService
import fr.laucoin.registry.backend.domain.service.IParticipantService
import fr.laucoin.registry.backend.domain.service.IUserService
import java.time.ZonedDateTime
import java.util.Objects
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
class ParticipantService(
    private val repository: IParticipantModelRepository,
    private val eventService: IEventService,
    private val userService: IUserService,
    private val groupRepository: IGroupModelRepository,
): IParticipantService, GenericService() {
    override fun findParticipantsByEventId(
        eventId: UUID,
        order: Direction,
        onlyVisible: Boolean,
        onlyPresent: Boolean,
        searched: String?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ): Flux<ParticipantModel> {
        return repository.findAll(eventId, onlyVisible, onlyPresent, startDateTime, endDateTime)
            .searchAndSort(order, searched, compareBy { it.lastName })
    }

    override fun findParticipantsByIds(eventId: UUID, ids: List<UUID>, onlyVisible: Boolean): Flux<ParticipantModel> {
        return repository.findAllByIds(eventId, ids, onlyVisible)
    }

    override fun findParticipantById(eventId: UUID, id: UUID, onlyVisible: Boolean): Mono<ParticipantModel> {
        return repository.findById(eventId, id, onlyVisible)
            .notFoundIfEmpty(id)
    }

    override fun searchUsers(eventId: UUID, searched: String?): Flux<UserModel> {
        return userService.findUsers(
            order = ASC,
            onlyVisible = true,
            searched
        )
    }

    override fun searchGroups(eventId: UUID, searched: String?): Flux<GroupModel> {
        return groupRepository.findAll(
            eventId,
            onlyVisible = true,
            onlyPresent = false,
            startDateTime = null,
            endDateTime = null
        ).searchAndSort(order = ASC, searched, compareBy { it.name })
    }

    override fun createParticipant(currentUser: UserModel, participant: ParticipantModel): Mono<ParticipantModel> {
        return eventService.validateDateTimes(
            participant.event !!.id !!,
            participant.begin,
            participant.end,
            PARTICIPANT_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE,
        )
            .flatMap {
                if (Objects.nonNull(participant.user?.id)) {
                    validateNoParticipantForUser(participant.event !!.id !!, participant.user !!.id !!)
                } else {
                    Mono.just(emptyList())
                }
            }
            .flatMap {
                if (participant.groups.isEmpty()) {
                    Mono.just(participant)
                } else {
                    validateGroups(participant.event !!.id !!, participant, participant.groups.mapNotNull { g -> g.id })
                }
            }
            .flatMap { repository.create(participant.apply { create(currentUser) }) }
    }

    private fun validateNoParticipantForUser(eventId: UUID, userId: UUID): Mono<List<ParticipantModel>> {
        return repository.findAll(
            eventId,
            onlyVisible = false,
            onlyPresent = false,
            startDateTime = null,
            endDateTime = null
        )
            .filter { it.user?.id == userId }
            .collectList()
            .handle { it, handle ->
                if (it.isNotEmpty()) {
                    val exception = RegistryException(
                        CONFLICT,
                        PARTICIPANT_IN_EVENT_ALREADY_LINKED_TO_USER,
                        arrayListOf("${it.first().firstName} ${it.first().lastName}")
                    )
                    log.error("Attempt to link an already link user to a participant", exception)
                    handle.error(exception)
                } else handle.next(it)
            }
    }

    override fun updateParticipantById(
        currentUser: UserModel,
        eventId: UUID,
        id: UUID,
        participant: ParticipantModel
    ): Mono<ParticipantModel> {
        return eventService.validateDateTimes(
            participant.event !!.id !!,
            participant.begin,
            participant.end,
            PARTICIPANT_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE,
        )
            .flatMap { findParticipantById(eventId, id, onlyVisible = false) }
            .flatMap { toUpdate ->
                if (toUpdate.user?.id != participant.user?.id && Objects.nonNull(participant.user?.id)) {
                    validateNoParticipantForUser(participant.event !!.id !!, participant.user !!.id !!)
                        .map { toUpdate }
                } else {
                    Mono.just(toUpdate)
                }
            }
            .flatMap {
                val newGroup: List<UUID> = it.getNewGroupIds(participant)
                if (newGroup.isEmpty()) {
                    Mono.just(it)
                } else {
                    validateGroups(participant.event !!.id !!, it, newGroup)
                }
            }
            .map {
                it.apply {
                    firstName = participant.firstName
                    lastName = participant.lastName
                    birthday = participant.birthday
                    groups = participant.groups
                    user = participant.user
                    begin = participant.begin
                    end = participant.end
                }
            }
            .updateParticipant(currentUser)
    }

    private fun validateGroups(eventId: UUID, participant: ParticipantModel, newGroupIds: List<UUID>): Mono<ParticipantModel> {
        return groupRepository.findAllByIds(eventId, newGroupIds, onlyVisible = false)
            .collectList()
            .handle { it, handle ->
                when {
                    it.size != newGroupIds.size -> handle.error(
                        RegistryException(
                            NOT_FOUND,
                            PARTICIPANT_GROUPS_NOT_FOUND_IN_PARTICIPANT_EVENT,
                        )
                    )

                    it.any { m -> m.isNotVisible() } -> handle.error(
                        RegistryException(
                            CONFLICT,
                            PARTICIPANT_GROUPS_NOT_VISIBLE,
                        )
                    )

                    else -> handle.next(participant)
                }
            }
    }

    override fun disableParticipantById(currentUser: UserModel, eventId: UUID, id: UUID): Mono<ParticipantModel> {
        return findParticipantById(eventId, id, onlyVisible = true)
            .validateNotLastGroupMember(PARTICIPANT_DISABLE_LAST_GROUP_MEMBER)
            .updateVisibility(visibility = false)
            .updateParticipant(currentUser)
    }

    override fun enableParticipantById(currentUser: UserModel, eventId: UUID, id: UUID): Mono<ParticipantModel> {
        return findParticipantById(eventId, id, onlyVisible = false)
            .updateVisibility(visibility = true)
            .updateParticipant(currentUser)
    }

    override fun deleteParticipantById(currentUser: UserModel, eventId: UUID, id: UUID): Mono<Void> {
        return findParticipantById(eventId, id, onlyVisible = false)
            .validateNotLastGroupMember(PARTICIPANT_DELETE_LAST_GROUP_MEMBER)
            .flatMap { repository.deleteById(it.id !!) }
    }

    private fun Mono<ParticipantModel>.updateParticipant(currentUser: UserModel) = flatMap {
        repository.update(it.apply { update(currentUser) })
    }

    private fun Mono<ParticipantModel>.validateNotLastGroupMember(error: String) = flatMap { participantToUpdate ->
        if (participantToUpdate.groups.isEmpty()) {
            return@flatMap Mono.just(participantToUpdate)
        }

        groupRepository.findAllByIds(
            participantToUpdate.event !!.id !!,
            participantToUpdate.groups.mapNotNull { it.id },
            onlyVisible = false
        )
            .filter { it.members.size == 1 }
            .collectList()
            .handle { it, handle ->
                if (it.isNotEmpty()) {
                    log.warn("The participant {} is the last member of the group(s)", participantToUpdate.id)
                    handle.error(RegistryException(FORBIDDEN, error))
                } else handle.next(participantToUpdate)
            }
    }
}
