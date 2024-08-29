package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.extension.ReactiveExt.currentUser
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageModel.Companion.paginate
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IUserController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.UserDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.UserDtoMapper
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class UserController(
    private val service: IUserService,
    private val roleService: IRoleService,
    private val userMapper: UserDtoMapper,
    @Value("\${registry.feature.user.searched.max-result}")
    private val maxSearchedUserResult: Long,
): IUserController {
    override fun findUsers(
        offset: Int,
        limit: Int,
        order: Direction,
        onlyVisible: Boolean,
        searched: String?
    ): Mono<PageModel<UserModel>> {
        return service.findUsers(order, onlyVisible, searched)
            .paginate(offset, limit)
    }

    override fun findUserById(@PathVariable id: UUID): Mono<UserModel> = service.findUserById(id, onlyVisible = false)

    override fun searchUsers(searched: String?): Flux<UserDto> {
        return currentUser().flatMapMany { service.findUsers(ASC, onlyVisible = true, searched) }
            .take(maxSearchedUserResult)
            .map(userMapper::toDto)
    }

    override fun getAssignableUserRoles(): Mono<List<String>> = currentUser().map { roleService.getAssignableUserRoles(it) }

    override fun updateUserRole(id: UUID, role: String?): Mono<UserModel> {
        return currentUser().flatMap { service.updateUserRoleById(it, id, role) }
    }

    override fun blockUserById(id: UUID): Mono<UserModel> {
        return currentUser().flatMap { service.blockUserById(it, id) }
    }

    override fun unblockUserById(id: UUID): Mono<UserModel> {
        return currentUser().flatMap { service.unblockUserById(it, id) }
    }

    override fun impersonateUserById(id: UUID): Mono<UserModel> {
        return currentUser().flatMap { service.impersonateUserById(it, id) }
    }

    override fun deleteUserById(id: UUID): Mono<Void> {
        return currentUser().flatMap { service.deleteUserById(it, id) }
    }
}
