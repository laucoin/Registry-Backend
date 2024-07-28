package com.laucoin.registry.domain.profile.controller.impl

import com.laucoin.registry.core.model.util.PageModel
import com.laucoin.registry.core.util.currentUser
import com.laucoin.registry.domain.profile.controller.IEventProfileController
import com.laucoin.registry.domain.profile.model.EnrichedProfileModel
import com.laucoin.registry.domain.profile.model.ProfileModel
import com.laucoin.registry.domain.profile.model.ProfilesCreationModel
import com.laucoin.registry.domain.profile.service.IEventProfileService
import java.security.Principal
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus.MULTI_STATUS
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class EventProfileController(
    private val service: IEventProfileService
): IEventProfileController {
    override fun getPage(
        eventId: UUID,
        pageIndex: Int,
        pageSize: Int,
        order: Sort.Direction,
        onlyNonBlocked: Boolean,
        onlyAccepted: Boolean,
        searched: String?,
        startAccess: LocalDateTime?,
        endAccess: LocalDateTime?,
    ): Mono<PageModel<EnrichedProfileModel>> =
        service.getPage(
            eventId = eventId,
            pageIndex = pageIndex,
            pageSize = pageSize,
            order = order,
            onlyNonBlocked = onlyNonBlocked,
            onlyAccepted = onlyAccepted,
            searched = searched,
            startAccess = startAccess,
            endAccess = endAccess,
        )

    override fun findById(eventId: UUID, id: UUID, principal: Principal): Mono<EnrichedProfileModel> =
        service.findById(principal.currentUser(), eventId, id)

    override fun getRoles(eventId: UUID, id: UUID?, principal: Principal): Mono<List<String?>> =
        service.getRoles(principal.currentUser(), eventId, id)

    override fun createSupportProfile(eventId: UUID, role: String, principal: Principal): Mono<ProfileModel> =
        service.createSupportProfile(principal.currentUser(), role, eventId)

    override fun create(
        eventId: UUID,
        profiles: ProfilesCreationModel,
        principal: Principal
    ): Mono<ResponseEntity<List<ProfileModel>>> {
        return service.createMultiple(principal.currentUser(), eventId, profiles)
            .collectList()
            .map {
                ResponseEntity.status(
                    if (it.size != profiles.users?.size) MULTI_STATUS
                    else OK
                ).body(it)
            }
    }

    override fun updateById(eventId: UUID, id: UUID, profile: ProfileModel, principal: Principal): Mono<ProfileModel> =
        service.updateById(principal.currentUser(), eventId, id, profile)

    override fun blockById(eventId: UUID, id: UUID, principal: Principal): Mono<ProfileModel> =
        service.blockById(principal.currentUser(), eventId, id)

    override fun unblockById(eventId: UUID, id: UUID, principal: Principal): Mono<ProfileModel> =
        service.unblockById(principal.currentUser(), eventId, id)

    override fun deleteById(eventId: UUID, id: UUID, principal: Principal): Mono<Void> =
        service.deleteById(principal.currentUser(), eventId, id)
}
