package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.service.IEventProfileService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IEventProfileController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto.Companion.paginate
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.CreatedEventProfilesReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.EventProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.EventProfileWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.EventProfilesWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.CreatedEventProfilesReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.EventProfileReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.EventProfileRoleReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.EventProfileStatusReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.PartialUserReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.EventProfileWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.EventProfilesWriterDtoMapper
import java.time.ZonedDateTime
import java.util.Locale
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
    private val readerMapper: EventProfileReaderDtoMapper,
    private val partialUserReaderMapper: PartialUserReaderDtoMapper,
    private val eventProfileRoleReaderMapper: EventProfileRoleReaderDtoMapper,
    private val eventProfileStatusReaderMapper: EventProfileStatusReaderDtoMapper,
    private val createdEventProfilesReaderMapper: CreatedEventProfilesReaderDtoMapper,
    private val writerMapper: EventProfileWriterDtoMapper,
    private val profilesWriterMapper: EventProfilesWriterDtoMapper,
    @Value("\${registry.feature.profile.searched.max-user-result}")
    private val maxUserResult: Long,
): IEventProfileController {
    override fun findEventProfiles(
        locale: Locale,
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
            .paginate(offset, limit)
            .map { readerMapper.toDtoPage(it, locale) }
    }

    override fun findEventProfileById(locale: Locale, eventId: UUID, id: UUID): Mono<EventProfileReaderDto> {
        return service.findEventProfileByEventIdAndId(eventId, id, onlyVisible = false)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun searchUsers(locale: Locale, eventId: UUID, searched: String?): Flux<PartialUserReaderDto> {
        return service.searchUsers(searched)
            .take(maxUserResult)
            .map { partialUserReaderMapper.toDto(it, locale) }
    }

    override fun getAssignableEventProfileRoles(currentUser: CurrentUserModel, locale: Locale, eventId: UUID): Flux<LabelDto> {
        return service.getAssignableEventRoles(currentUser, eventId)
            .map { eventProfileRoleReaderMapper.toDto(it, locale) }
    }

    override fun getAvailableEventProfileStatus(locale: Locale, eventId: UUID): Flux<LabelDto> {
        return service.getAvailableEventStatus(eventId)
            .map { eventProfileStatusReaderMapper.toDto(it, locale) }
    }

    override fun createEventProfiles(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        profiles: EventProfilesWriterDto
    ): Mono<ResponseEntity<CreatedEventProfilesReaderDto>> {
        return service.createEventProfiles(currentUser, eventId, profiles.userIds !!, profilesWriterMapper.toModels(profiles, eventId))
            .map { createdEventProfilesReaderMapper.toDto(it, locale) }
            .map { ResponseEntity.status(if (it.notCreatedUserIds.isEmpty()) OK else MULTI_STATUS).body(it) }
    }

    override fun createSupportEventProfile(currentUser: CurrentUserModel, locale: Locale, eventId: UUID): Mono<EventProfileReaderDto> {
        return service.createSupportEventProfile(currentUser, eventId)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun updateEventProfile(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        id: UUID,
        profile: EventProfileWriterDto,
    ): Mono<EventProfileReaderDto> {
        return service.updateEventProfileById(currentUser, eventId, id, writerMapper.toModel(profile, eventId))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun blockEventProfileById(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        id: UUID,
    ): Mono<EventProfileReaderDto> {
        return service.blockEventProfileById(currentUser, eventId, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun unblockEventProfileById(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        id: UUID,
    ): Mono<EventProfileReaderDto> {
        return service.unblockEventProfileById(currentUser, eventId, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun deleteEventProfileById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<Void> {
        return service.deleteEventProfileById(currentUser, eventId, id)
    }
}
