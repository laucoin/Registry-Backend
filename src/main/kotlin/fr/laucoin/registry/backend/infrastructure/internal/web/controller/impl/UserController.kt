package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IUserController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto.Companion.paginate
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class UserController(
    private val service: IUserService,
    private val roleService: IRoleService,
): IUserController {
    override fun findUsers(
        offset: Int,
        limit: Int,
        order: Direction,
        onlyVisible: Boolean,
        searched: String?
    ): Mono<PageDto<UserModel>> {
        return service.findUsers(order, onlyVisible, searched)
            .paginate(offset, limit)
    }

    override fun findUserById(@PathVariable id: UUID): Mono<UserModel> = service.findUserById(id, onlyVisible = false)

    override fun getAssignableUserRoles(currentUser: CurrentUserModel): Mono<List<String>> =
        Mono.just(roleService.getAssignableUserRoles(currentUser))

    override fun updateUserRole(currentUser: CurrentUserModel, id: UUID, role: String?): Mono<UserModel> {
        return service.updateUserRoleById(currentUser, id, role)
    }

    override fun blockUserById(currentUser: CurrentUserModel, id: UUID): Mono<UserModel> {
        return service.blockUserById(currentUser, id)
    }

    override fun unblockUserById(currentUser: CurrentUserModel, id: UUID): Mono<UserModel> {
        return service.unblockUserById(currentUser, id)
    }

    override fun impersonateUserById(currentUser: CurrentUserModel, id: UUID): Mono<UserModel> {
        return service.impersonateUserById(currentUser, id)
    }

    override fun deleteUserById(currentUser: CurrentUserModel, id: UUID): Mono<Void> {
        return service.deleteUserById(currentUser, id)
    }
}
