package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.currentUser
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageModel.Companion.paginate
import fr.laucoin.registry.backend.domain.service.IUserEventProfileService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IUserEventProfileController
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class UserEventProfileController(
    private val service: IUserEventProfileService,
): IUserEventProfileController {
    override fun findUserEventProfiles(
        offset: Int,
        limit: Int,
        order: Direction,
        onlyVisible: Boolean,
        onlyUsable: Boolean,
        status: ProfileStatusEnum?,
        searched: String?,
        startAccess: ZonedDateTime?,
        endAccess: ZonedDateTime?
    ): Mono<PageModel<EventProfileModel>> {
        return currentUser().flatMap {
            service.findUserEventProfiles(it.id !!, order, onlyVisible, onlyUsable, status, searched, startAccess, endAccess)
                .paginate(offset, limit)
        }
    }

    override fun findUserEventProfileById(id: UUID): Mono<EventProfileModel> {
        return currentUser().flatMap { service.findUserEventProfileById(it, id, onlyVisible = false) }
    }

    override fun manageUserEventProfileAcceptance(id: UUID, status: ProfileStatusEnum): Mono<EventProfileModel> {
        return currentUser().flatMap { service.updateUserEventProfileStatusById(it, id, status) }
    }

    override fun deleteUserProfileById(id: UUID): Mono<Void> {
        return currentUser().flatMap { service.deleteUserEventProfileById(it, id) }
    }
}
