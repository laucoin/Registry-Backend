package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_DELETE_HAS_MOVEMENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_DELETE_LAST_GROUP_MEMBER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_DISABLE_LAST_GROUP_MEMBER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_GROUPS_NOT_FOUND_IN_PARTICIPANT_EVENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_GROUPS_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_IN_EVENT_ALREADY_LINKED_TO_USER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.repository.IGroupModelRepository
import fr.laucoin.registry.backend.domain.repository.IMovementModelRepository
import fr.laucoin.registry.backend.domain.repository.IParticipantModelRepository
import fr.laucoin.registry.backend.domain.repository.IUserModelRepository
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IEventService
import fr.laucoin.registry.backend.domain.service.IParticipantService
import java.util.Objects
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class ParticipantService(
    private val eventService: IEventService,
    private val repository: IParticipantModelRepository,
    private val userRepository: IUserModelRepository,
    private val movementRepository: IMovementModelRepository,
    private val groupRepository: IGroupModelRepository,
    @Value("\${registry.feature.participant.searched.max-user-result}")
    private val maxUserResult: Int,
    @Value("\${registry.feature.participant.searched.max-group-result}")
    private val maxGroupResult: Int,
): IParticipantService, GenericService() {
    override fun findParticipantsPage(
        eventId: UUID,
        pageable: PageableModel,
        searchParams: ParticipantSearchParamModel,
    ): Mono<PageModel<ParticipantModel>> {
        return repository.findPage(eventId, pageable, searchParams)
    }

    override fun findParticipantsByIds(eventId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<ParticipantModel> {
        return repository.findAllByIds(eventId, ids, visibilitySearched)
    }

    override fun findParticipantById(eventId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<ParticipantModel> {
        return repository.findById(eventId, id, visibilitySearched)
            .notFoundIfEmpty(id)
    }

    override fun searchUsers(eventId: UUID, textSearched: String?): Flux<UserModel> {
        return userRepository.findWithLimit(
            maxUserResult,
            UserSearchParamModel(textSearched, visibilitySearched = true),
        )
    }

    override fun searchGroups(eventId: UUID, textSearched: String?): Flux<GroupModel> {
        return groupRepository.findWithLimit(
            maxGroupResult,
            eventId,
            GroupSearchParamModel(textSearched, visibilitySearched = true),
        )
    }

    override fun findParticipantMovementsPage(
        eventId: UUID,
        id: UUID,
        pageable: PageableModel,
        searchParams: MovementSearchParamModel
    ): Mono<PageModel<MovementModel>> {
        return movementRepository.findPageByParticipantId(
            eventId,
            id,
            pageable,
            searchParams
        )
    }

    override fun createParticipant(currentUser: CurrentUserModel, participant: ParticipantModel): Mono<ParticipantModel> {
        return eventService.validateDateTimes(
            participant.event !!.id !!,
            participant.startAvailability,
            participant.endAvailability,
            PARTICIPANT_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE,
        )
            .flatMap {
                if (Objects.nonNull(participant.user)) {
                    validateNoParticipantForUser(participant.event !!.id !!, participant.user !!.id !!)
                } else Mono.just(emptyList())
            }
            .flatMap {
                if (participant.groups.isNotEmpty()) {
                    validateGroups(participant.event !!.id !!, participant, participant.groups.mapNotNull { g -> g.id })
                } else Mono.just(participant)
            }
            .flatMap { repository.create(participant.apply { create(currentUser) }) }
    }

    private fun validateNoParticipantForUser(eventId: UUID, userId: UUID): Mono<List<ParticipantModel>> {
        return repository.findByUserId(eventId, userId)
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

    private fun validateGroups(eventId: UUID, participant: ParticipantModel, newGroupIds: List<UUID>): Mono<ParticipantModel> {
        return groupRepository.findAllByIds(eventId, newGroupIds, visibilitySearched = null)
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

    override fun updateParticipantById(
        currentUser: CurrentUserModel,
        eventId: UUID,
        id: UUID,
        participant: ParticipantModel
    ): Mono<ParticipantModel> {
        return eventService.validateDateTimes(
            participant.event !!.id !!,
            participant.startAvailability,
            participant.endAvailability,
            PARTICIPANT_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE,
        )
            .flatMap { findParticipantById(eventId, id, visibilitySearched = null) }
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
                    startAvailability = participant.startAvailability
                    endAvailability = participant.endAvailability
                }
            }
            .updateParticipant(currentUser)
    }

    private fun Mono<ParticipantModel>.updateParticipant(currentUser: CurrentUserModel) = flatMap {
        repository.update(it.apply { update(currentUser) })
    }

    override fun disableParticipantById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<ParticipantModel> {
        return findParticipantById(eventId, id, visibilitySearched = true)
            .validateNotLastGroupMember(PARTICIPANT_DISABLE_LAST_GROUP_MEMBER)
            .updateVisibility(visibility = false)
            .updateParticipant(currentUser)
    }

    override fun enableParticipantById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<ParticipantModel> {
        return findParticipantById(eventId, id, visibilitySearched = false)
            .updateVisibility(visibility = true)
            .updateParticipant(currentUser)
    }

    override fun deleteParticipantById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<Void> {
        return findParticipantById(eventId, id, visibilitySearched = null)
            .validateHasNoMovementLinked(PARTICIPANT_DELETE_HAS_MOVEMENT)
            .validateNotLastGroupMember(PARTICIPANT_DELETE_LAST_GROUP_MEMBER)
            .flatMap { repository.deleteById(it.id !!) }
    }

    private fun Mono<ParticipantModel>.validateHasNoMovementLinked(error: String) = flatMap { participantToUpdate ->
        movementRepository.countAllByParticipantId(
            participantToUpdate.event !!.id !!,
            participantToUpdate.id !!,
        ).handle { it, handle ->
            if (it > 0) {
                log.warn("The participant {} already linked to movement(s)", participantToUpdate.id)
                handle.error(RegistryException(FORBIDDEN, error))
            } else handle.next(participantToUpdate)
        }
    }

    private fun Mono<ParticipantModel>.validateNotLastGroupMember(error: String) = flatMap { participantToUpdate ->
        if (participantToUpdate.groups.isEmpty()) {
            return@flatMap Mono.just(participantToUpdate)
        }

        groupRepository.findAllByIds(
            participantToUpdate.event !!.id !!,
            participantToUpdate.groups.mapNotNull { it.id },
            visibilitySearched = null
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
