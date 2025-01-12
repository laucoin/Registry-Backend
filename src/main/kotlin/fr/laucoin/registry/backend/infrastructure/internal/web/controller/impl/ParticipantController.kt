package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.extension.ReactiveExt.currentUser
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageModel.Companion.paginate
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.service.IParticipantService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IParticipantController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.ParticipantDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.UserDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.ParticipantDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.UserDtoMapper
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort.Direction
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class ParticipantController(
    private val service: IParticipantService,
    private val mapper: ParticipantDtoMapper,
    private val userMapper: UserDtoMapper,
    @Value("\${registry.feature.participant.searched.max-user-result}")
    private val maxUserResult: Long,
    @Value("\${registry.feature.participant.searched.max-group-result}")
    private val maxGroupResult: Long,
): IParticipantController {
    override fun findParticipants(
        eventId: UUID,
        offset: Int,
        limit: Int,
        order: Direction,
        onlyVisible: Boolean,
        onlyPresent: Boolean,
        searched: String?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ): Mono<PageModel<ParticipantModel>> {
        return service.findParticipantsByEventId(
            eventId,
            order,
            onlyVisible,
            onlyPresent,
            searched,
            startDateTime,
            endDateTime
        ).paginate(offset, limit)
    }

    override fun findParticipantById(eventId: UUID, id: UUID): Mono<ParticipantModel> {
        return service.findParticipantById(eventId, id, onlyVisible = false)
    }

    override fun searchUsers(eventId: UUID, searched: String?): Flux<UserDto> {
        return service.searchUsers(eventId, searched)
            .take(maxUserResult)
            .map(userMapper::toDto)
    }

    override fun searchGroups(eventId: UUID, searched: String?): Flux<GroupModel> {
        return service.searchGroups(eventId, searched)
            .take(maxGroupResult)
    }

    override fun createParticipant(eventId: UUID, participant: ParticipantDto): Mono<ParticipantModel> {
        return currentUser().flatMap { service.createParticipant(it, mapper.toModel(participant, eventId)) }
    }

    override fun updateParticipantById(
        eventId: UUID,
        id: UUID,
        participant: ParticipantDto
    ): Mono<ParticipantModel> {
        return currentUser().flatMap { service.updateParticipantById(it, eventId, id, mapper.toModel(participant, eventId)) }
    }

    override fun disableParticipantById(eventId: UUID, id: UUID): Mono<ParticipantModel> {
        return currentUser().flatMap { service.disableParticipantById(it, eventId, id) }
    }

    override fun enableParticipantById(eventId: UUID, id: UUID): Mono<ParticipantModel> {
        return currentUser().flatMap { service.enableParticipantById(it, eventId, id) }
    }

    override fun deleteParticipantById(eventId: UUID, id: UUID): Mono<Void> {
        return currentUser().flatMap { service.deleteParticipantById(it, eventId, id) }
    }
}
