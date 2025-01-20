package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.service.IEventProfileService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IEventProfileController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto.Companion.paginate
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.CreatedEventProfilesReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.EventProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.EventProfileWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.EventProfilesWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.EventProfileReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.PartialUserReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.EventProfileWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.EventProfilesWriterDtoMapper
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
    private val userMapper: PartialUserReaderDtoMapper,
    private val profileReaderMapper: EventProfileReaderDtoMapper,
    private val profilesWriterMapper: EventProfilesWriterDtoMapper,
    private val profileWriterMapper: EventProfileWriterDtoMapper,
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
    ): Mono<PageDto<EventProfileReaderDto>> {
        return service.findEventProfilesByEventId(
            eventId,
            order,
            onlyVisible,
            status,
            searched,
            startAccess,
            endAccess
        )
            .map(profileReaderMapper::toDto)
            .paginate(offset, limit)
    }

    override fun findEventProfileById(eventId: UUID, id: UUID): Mono<EventProfileReaderDto> {
        return service.findEventProfileByEventIdAndId(eventId, id, onlyVisible = false)
            .map(profileReaderMapper::toDto)
    }

    override fun searchUsers(eventId: UUID, searched: String?): Flux<PartialUserReaderDto> {
        return service.searchUsers(searched)
            .take(maxUserResult)
            .map(userMapper::toDto)
    }

    override fun getAssignableEventProfileRoles(currentUser: CurrentUserModel, eventId: UUID): Mono<List<String>> {
        return service.getAssignableEventRoles(currentUser, eventId)
    }

    override fun createEventProfiles(
        currentUser: CurrentUserModel,
        eventId: UUID,
        profiles: EventProfilesWriterDto
    ): Mono<ResponseEntity<CreatedEventProfilesReaderDto>> {
        return service.createEventProfiles(currentUser, eventId, profiles.userIds !!, profilesWriterMapper.toModels(profiles, eventId))
            .collectList()
            .map { CreatedEventProfilesReaderDto(it) }
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

    override fun createSupportEventProfile(currentUser: CurrentUserModel, eventId: UUID): Mono<EventProfileModel> {
        return service.createSupportEventProfile(currentUser, eventId)
    }

    override fun updateEventProfile(
        currentUser: CurrentUserModel,
        eventId: UUID,
        id: UUID,
        profile: EventProfileWriterDto,
    ): Mono<EventProfileModel> {
        return service.updateEventProfileById(currentUser, eventId, id, profileWriterMapper.toModel(profile, eventId))
    }

    override fun blockEventProfileById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<EventProfileModel> {
        return service.blockEventProfileById(currentUser, eventId, id)
    }

    override fun unblockEventProfileById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<EventProfileModel> {
        return service.unblockEventProfileById(currentUser, eventId, id)
    }

    override fun deleteEventProfileById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<Void> {
        return service.deleteEventProfileById(currentUser, eventId, id)
    }
}
