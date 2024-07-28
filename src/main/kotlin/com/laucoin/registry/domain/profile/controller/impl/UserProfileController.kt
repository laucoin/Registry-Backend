package com.laucoin.registry.domain.profile.controller.impl

import com.laucoin.registry.core.model.util.PageModel
import com.laucoin.registry.core.util.currentUser
import com.laucoin.registry.domain.profile.controller.IUserProfileController
import com.laucoin.registry.domain.profile.model.EnrichedProfileModel
import com.laucoin.registry.domain.profile.model.ProfileModel
import com.laucoin.registry.domain.profile.service.impl.UserProfileService
import java.security.Principal
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class UserProfileController(
    private val service: UserProfileService
): IUserProfileController {
    override fun getPage(
        userId: UUID,
        pageIndex: Int,
        pageSize: Int,
        order: Sort.Direction,
        onlyAccepted: Boolean,
        searched: String?,
        startAccess: LocalDateTime?,
        endAccess: LocalDateTime?,
    ): Mono<PageModel<EnrichedProfileModel>> = service.getPage(
        userId = userId,
        pageIndex = pageIndex,
        pageSize = pageSize,
        order = order,
        onlyAccepted = onlyAccepted,
        searched = searched,
        startAccess = startAccess,
        endAccess = endAccess,
    )

    override fun findById(userId: UUID, id: UUID, principal: Principal): Mono<EnrichedProfileModel> =
        service.findById(principal.currentUser(), userId, id)

    override fun manageProfileAcceptance(userId: UUID, id: UUID, accepted: Boolean, principal: Principal): Mono<ProfileModel> =
        service.manageProfileAcceptance(principal.currentUser(), userId, id, accepted)

    override fun deleteById(userId: UUID, id: UUID, principal: Principal): Mono<Void> =
        service.deleteById(principal.currentUser(), userId, id)
}
