package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.repository.IUserModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.CurrentUserEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.UserEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IUserEntityRepository
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class UserModelPostgresRepository(
    private val repository: IUserEntityRepository,
    private val userMapper: UserEntityMapper,
    private val currentUserMapper: CurrentUserEntityMapper,
): IUserModelRepository {
    override fun findAll(onlyVisible: Boolean): Flux<UserModel> = repository.findAll(onlyVisible).map(userMapper::toModel)
    override fun findById(id: UUID, onlyVisible: Boolean): Mono<UserModel> =
        repository.findById(id, onlyVisible).map(userMapper::toModel)

    override fun findByOidcId(oidcId: UUID, onlyVisible: Boolean): Mono<CurrentUserModel> =
        repository.findByOidcId(oidcId, onlyVisible).map(currentUserMapper::toModel)

    override fun findServiceAccount(): Mono<UserModel> = repository.findServiceAccount().map(userMapper::toModel)

    override fun findByRoleLevel(roleLevel: Int, onlyVisible: Boolean): Flux<UserModel> =
        repository.findByRoleLevel(roleLevel, onlyVisible).map(userMapper::toModel)

    override fun create(element: UserModel): Mono<UserModel> = save(element)
    override fun update(element: UserModel): Mono<UserModel> = save(element)
    private fun save(element: UserModel): Mono<UserModel> = repository.save(userMapper.toEntity(element)).map(userMapper::toModel)
    override fun deleteById(id: UUID): Mono<Void> = repository.deleteById(id)
}
