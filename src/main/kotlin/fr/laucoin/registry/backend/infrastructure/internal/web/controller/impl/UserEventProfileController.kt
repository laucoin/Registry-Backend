package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.service.IUserEventProfileService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IUserEventProfileController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto.Companion.paginate
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.EventProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.EventProfileReaderDtoMapper
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class UserEventProfileController(
    private val service: IUserEventProfileService,
    private val readerMapper: EventProfileReaderDtoMapper,
): IUserEventProfileController {
    override fun findUserEventProfiles(
        currentUser: CurrentUserModel,
        offset: Int,
        limit: Int,
        order: Direction,
        onlyVisible: Boolean,
        onlyUsable: Boolean,
        status: ProfileStatusEnum?,
        searched: String?,
        startAccess: ZonedDateTime?,
        endAccess: ZonedDateTime?
    ): Mono<PageDto<EventProfileReaderDto>> {
        return service.findUserEventProfiles(
            currentUser.id !!, order, onlyVisible, onlyUsable, status, searched, startAccess, endAccess
        )
            .map(readerMapper::toDto)
            .paginate(offset, limit)
    }

    override fun findUserEventProfileById(currentUser: CurrentUserModel, id: UUID): Mono<EventProfileReaderDto> {
        return service.findUserEventProfileById(currentUser, id, onlyVisible = false)
            .map(readerMapper::toDto)
    }

    override fun manageUserEventProfileAcceptance(
        currentUser: CurrentUserModel,
        id: UUID,
        status: ProfileStatusEnum,
    ): Mono<EventProfileModel> {
        return service.updateUserEventProfileStatusById(currentUser, id, status)
    }

    override fun deleteUserProfileById(currentUser: CurrentUserModel, id: UUID): Mono<Void> {
        return service.deleteUserEventProfileById(currentUser, id)
    }
}
