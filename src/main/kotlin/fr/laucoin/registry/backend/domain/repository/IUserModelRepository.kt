package fr.laucoin.registry.backend.domain.repository

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IUserModelRepository: IGenericReadModelRepository<UserModel>, IGenericWriteModelRepository<UserModel> {
    fun findPage(pageable: PageableModel, searchParams: UserSearchParamModel): Mono<PageModel<UserModel>>
    fun findWithLimit(limit: Int, searchParams: UserSearchParamModel): Flux<UserModel>
    fun findByOidcId(oidcId: UUID, visibilitySearched: Boolean?): Mono<CurrentUserModel>
    fun findServiceAccount(): Mono<CurrentUserModel>
    fun findByRoleLevel(roleLevel: Int, visibilitySearched: Boolean?): Flux<UserModel>
}
