package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_IN_EVENT_ALREADY_LINKED_TO_USER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.RegistryExceptionModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.repository.IParticipantModelRepository
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IEventService
import fr.laucoin.registry.backend.domain.service.IParticipantService
import java.time.ZonedDateTime
import java.util.Objects
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class ParticipantService(
    private val repository: IParticipantModelRepository,
    private val eventService: IEventService,
): IParticipantService, GenericService<ParticipantModel>(compareBy { it.lastName }) {
    override fun findParticipantsByEventId(
        eventId: UUID,
        order: Direction,
        onlyVisible: Boolean,
        onlyPresent: Boolean,
        searched: String?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ): Flux<ParticipantModel> {
        return repository.findAll(eventId, onlyVisible, startDateTime, endDateTime)
            .searchAndSort(order, searched)
    }

    override fun findParticipantById(eventId: UUID, id: UUID, onlyVisible: Boolean): Mono<ParticipantModel> {
        return repository.findById(eventId, id, onlyVisible)
            .notFoundIfEmpty(id)
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
            .flatMap { repository.save(participant.apply { create(currentUser) }) }
    }

    private fun validateNoParticipantForUser(eventId: UUID, userId: UUID): Mono<List<ParticipantModel>> {
        return findParticipantsByEventId(
            eventId,
            ASC,
            onlyVisible = false,
            onlyPresent = false,
            searched = null,
            startDateTime = null,
            endDateTime = null
        )
            .filter { it.user?.id == userId }
            .collectList()
            .handle { it, handle ->
                if (it.isNotEmpty()) {
                    val exception = RegistryExceptionModel(CONFLICT, PARTICIPANT_IN_EVENT_ALREADY_LINKED_TO_USER)
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
            .map {
                it.apply {
                    firstName = participant.firstName
                    lastName = participant.lastName
                    birthday = participant.birthday
                    user = participant.user
                    begin = participant.begin
                    end = participant.end
                }
            }
            .updateParticipant(currentUser)
    }

    override fun disableParticipantById(currentUser: UserModel, eventId: UUID, id: UUID): Mono<ParticipantModel> {
        return findParticipantById(eventId, id, onlyVisible = true)
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
            .flatMap { repository.deleteById(it.id !!) }
    }

    private fun Mono<ParticipantModel>.updateParticipant(currentUser: UserModel) = flatMap {
        repository.save(it.apply { update(currentUser) })
    }
}
