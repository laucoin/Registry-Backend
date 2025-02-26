package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.EventProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.IUserEventProfileService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IUserEventProfileController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.EventProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.EventProfileReaderDtoMapper
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class UserEventProfileController(
    private val service: IUserEventProfileService,
    private val readerMapper: EventProfileReaderDtoMapper,
): IUserEventProfileController {
    override fun findUserEventProfiles(
        currentUser: CurrentUserModel,
        locale: Locale,
        pageNumber: Int,
        pageSize: Int,
        textSearched: String?,
        availabilitySearched: Boolean?,
        statusSearched: ProfileStatusEnum?,
        dateTimeSearched: ZonedDateTime?,
    ): Mono<PageModel<EventProfileReaderDto>> {
        return service.findEventProfilesPage(
            currentUser.id !!,
            PageableModel(pageNumber * pageSize, pageSize),
            EventProfileSearchParamModel(textSearched, availabilitySearched, statusSearched, dateTimeSearched),
        ).map { readerMapper.toDtoPage(it, locale) }
    }

    override fun findUserEventProfileById(currentUser: CurrentUserModel, locale: Locale, id: UUID): Mono<EventProfileReaderDto> {
        return service.findUserEventProfileById(currentUser, id, visibilitySearched = null)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun manageUserEventProfileAcceptance(
        currentUser: CurrentUserModel,
        locale: Locale,
        id: UUID,
        accepted: Boolean,
    ): Mono<EventProfileReaderDto> {
        return service.updateUserEventProfileStatusById(
            currentUser,
            id,
            if (accepted) ProfileStatusEnum.ACCEPTED else ProfileStatusEnum.REJECTED
        ).map { readerMapper.toDto(it, locale) }
    }

    override fun createSupportEventProfile(currentUser: CurrentUserModel, locale: Locale, eventId: UUID): Mono<EventProfileReaderDto> {
        return service.createSupportEventProfile(currentUser, eventId)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun deleteUserProfileById(currentUser: CurrentUserModel, id: UUID): Mono<Void> {
        return service.deleteUserEventProfileById(currentUser, id)
    }
}
