package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.UserModel
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IUserService {
    fun findUsers(order: Direction, onlyVisible: Boolean, searched: String?): Flux<UserModel>
    fun findUserById(id: UUID, onlyVisible: Boolean): Mono<UserModel>
    fun findUserByOidcId(id: UUID, onlyVisible: Boolean): Mono<CurrentUserModel>
    fun getServiceAccount(): UserModel
    fun getAssignableUserRoles(currentUser: CurrentUserModel): Flux<String>
    fun createUser(oidcId: UUID, email: String, firstName: String?, lastName: String?): Mono<CurrentUserModel>
    fun updateUserIfPersonalDataChanged(
        user: CurrentUserModel,
        email: String,
        firstName: String?,
        lastName: String?
    ): Mono<CurrentUserModel>

    fun updateUserRoleById(currentUser: UserModel, id: UUID, role: String?): Mono<UserModel>
    fun blockUserById(currentUser: UserModel, id: UUID): Mono<UserModel>
    fun unblockUserById(currentUser: UserModel, id: UUID): Mono<UserModel>
    fun impersonateUserById(currentUser: UserModel, id: UUID): Mono<UserModel>
    fun deleteUserById(currentUser: UserModel, id: UUID): Mono<Void>
}
