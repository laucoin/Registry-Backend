package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.currentUser
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageModel.Companion.paginate
import fr.laucoin.registry.backend.domain.service.IEventProfileService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IEventProfileController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.CreatedEventProfilesDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.EventProfileDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.EventProfilesDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.UserDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.EventProfileDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.EventProfilesDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.UserDtoMapper
import java.time.ZonedDateTime
import java.util.Objects
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
class EventProfileController(
    private val service: IEventProfileService,
    private val profilesMapper: EventProfilesDtoMapper,
    private val profileMapper: EventProfileDtoMapper,
    private val userMapper: UserDtoMapper,
    @Value("\${registry.feature.profile.searched.max-user-result}")
    private val maxUserResult: Long,
): IEventProfileController {
    override fun findEventProfiles(
        eventId: UUID,
        offset: Int,
        limit: Int,
        order: Direction,
        onlyVisible: Boolean,
        status: ProfileStatusEnum?,
        searched: String?,
        startAccess: ZonedDateTime?,
        endAccess: ZonedDateTime?
    ): Mono<PageModel<EventProfileModel>> {
        return service.findEventProfilesByEventId(
            eventId,
            order,
            onlyVisible,
            status,
            searched,
            startAccess,
            endAccess
        ).paginate(offset, limit)
    }

    override fun findEventProfileById(eventId: UUID, id: UUID): Mono<EventProfileModel> {
        return service.findEventProfileByEventIdAndId(eventId, id, onlyVisible = false)
    }

    override fun searchUsers(eventId: UUID, searched: String?): Flux<UserDto> {
        return service.searchUsers(searched)
            .take(maxUserResult)
            .map(userMapper::toDto)
    }

    override fun getAssignableEventProfileRoles(eventId: UUID): Mono<List<String>> {
        return currentUser().flatMap { service.getAssignableEventRoles(it, eventId) }
    }

    override fun createEventProfiles(eventId: UUID, profiles: EventProfilesDto): Mono<ResponseEntity<CreatedEventProfilesDto>> {
        return currentUser().flatMapMany {
            service.createEventProfiles(it, eventId, profiles.userIds !!, profilesMapper.toModels(profiles, eventId))
        }.collectList()
            .map { CreatedEventProfilesDto(it) }
            .map { body ->
                if (body.profiles.size == profiles.userIds !!.size) {
                    ResponseEntity.status(OK).body(body)
                } else {
                    body.notCreatedUserIds = profiles.userIds !!.filter {
                        Objects.isNull(body.profiles.find { profile -> profile.user !!.id == it })
                    }
                    ResponseEntity.status(MULTI_STATUS).body(body)
                }
            }
    }

    override fun createSupportEventProfile(eventId: UUID): Mono<EventProfileModel> {
        return currentUser().flatMap { service.createSupportEventProfile(it, eventId) }
    }

    override fun updateEventProfile(eventId: UUID, id: UUID, profile: EventProfileDto): Mono<EventProfileModel> {
        return currentUser().flatMap { service.updateEventProfileById(it, eventId, id, profileMapper.toModel(profile, eventId)) }
    }

    override fun blockEventProfileById(eventId: UUID, id: UUID): Mono<EventProfileModel> {
        return currentUser().flatMap { service.blockEventProfileById(it, eventId, id) }
    }

    override fun unblockEventProfileById(eventId: UUID, id: UUID): Mono<EventProfileModel> {
        return currentUser().flatMap { service.unblockEventProfileById(it, eventId, id) }
    }

    override fun deleteEventProfileById(eventId: UUID, id: UUID): Mono<Void> {
        return currentUser().flatMap { service.deleteEventProfileById(it, eventId, id) }
    }
}
