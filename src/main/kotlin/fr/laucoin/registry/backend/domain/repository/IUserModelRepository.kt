package fr.laucoin.registry.backend.domain.repository

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.UserModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IUserModelRepository: IGenericReadModelRepository<UserModel>, IGenericWriteModelRepository<UserModel> {
    fun findAll(onlyVisible: Boolean): Flux<UserModel>
    fun findByOidcId(oidcId: UUID, onlyVisible: Boolean): Mono<CurrentUserModel>
    fun findServiceAccount(): Mono<UserModel>
    fun findByRoleLevel(roleLevel: Int, onlyVisible: Boolean): Flux<UserModel>
}
