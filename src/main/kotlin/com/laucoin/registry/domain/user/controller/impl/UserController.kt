package com.laucoin.registry.domain.user.controller.impl

import com.laucoin.registry.core.model.user.EnrichedUserModel
import com.laucoin.registry.core.model.user.UserModel
import com.laucoin.registry.core.model.util.PageModel
import com.laucoin.registry.core.util.currentUser
import com.laucoin.registry.domain.user.controller.IUserController
import com.laucoin.registry.domain.user.service.IUserService
import java.security.Principal
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class UserController(
    private val service: IUserService
): IUserController {
    override fun getPage(
        pageIndex: Int,
        pageSize: Int,
        order: Direction,
        onlyNonBlocked: Boolean,
        searched: String?,
        principal: Principal
    ): Mono<PageModel<EnrichedUserModel>> =
        service.getPage(
            pageIndex = pageIndex,
            pageSize = pageSize,
            order = order,
            onlyNonBlocked = onlyNonBlocked,
            searched = searched,
        )

    override fun getMe(principal: Principal): EnrichedUserModel = principal.currentUser()

    override fun getRoles(principal: Principal): Mono<List<String?>> =
        service.getRoles(principal.currentUser())

    override fun findById(id: UUID, principal: Principal): Mono<EnrichedUserModel> =
        service.findById(id)

    override fun findAccountEmailBySearch(searched: String, principal: Principal): Mono<List<String?>> =
        service.findByEmail(principal.currentUser(), searched)

    override fun updateRole(id: UUID, role: String?, principal: Principal): Mono<UserModel> =
        service.updateRoleById(principal.currentUser(), id, role)

    override fun updateDefaultProfile(id: UUID, profileId: UUID, principal: Principal): Mono<UserModel> =
        service.updateDefaultProfileById(principal.currentUser(), id, profileId)

    override fun blockById(id: UUID, principal: Principal): Mono<UserModel> =
        service.blockById(principal.currentUser(), id)

    override fun unblockById(id: UUID, principal: Principal): Mono<UserModel> =
        service.unblockById(principal.currentUser(), id)

    override fun deleteById(id: UUID, principal: Principal): Mono<Void> =
        service.deleteById(principal.currentUser(), id)
}
