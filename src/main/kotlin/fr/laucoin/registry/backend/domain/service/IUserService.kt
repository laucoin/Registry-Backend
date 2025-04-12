package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IUserService {
    fun findUsersPage(pageable: PageableModel, searchParams: UserSearchParamModel): Mono<PageModel<UserModel>>
    fun findUserById(id: UUID, visibilitySearched: Boolean?): Mono<UserModel>
    fun findUserByOidcId(id: UUID, visibilitySearched: Boolean?): Mono<CurrentUserModel>
    fun serviceAccount(): UserModel
    fun assignableUserRoles(currentUser: CurrentUserModel): Flux<String>
    fun createUser(oidcId: UUID, email: String, firstName: String?, lastName: String?): Mono<CurrentUserModel>
    fun updateUserIfPersonalDataChanged(
        user: CurrentUserModel,
        email: String,
        firstName: String?,
        lastName: String?
    ): Mono<CurrentUserModel>

    fun updateUserRoleById(currentUser: CurrentUserModel, id: UUID, role: String?): Mono<UserModel>
    fun blockUserById(currentUser: CurrentUserModel, id: UUID): Mono<UserModel>
    fun unblockUserById(currentUser: CurrentUserModel, id: UUID): Mono<UserModel>
    fun impersonateUserById(currentUser: CurrentUserModel, id: UUID): Mono<UserModel>
    fun deleteUserById(currentUser: CurrentUserModel, id: UUID): Mono<Void>
}
