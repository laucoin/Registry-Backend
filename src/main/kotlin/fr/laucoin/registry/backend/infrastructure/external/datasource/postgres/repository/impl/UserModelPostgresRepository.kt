package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.repository.IUserModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.CurrentUserEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.UserEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IUserEntityRepository
import java.time.LocalDate
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class UserModelPostgresRepository(
    private val repository: IUserEntityRepository,
    private val mapper: UserEntityMapper,
    private val currentUserMapper: CurrentUserEntityMapper,
): IUserModelRepository {
    override fun findPage(pageable: PageableModel, searchParams: UserSearchParamModel): Mono<PageModel<UserModel>> {
        return Mono.zip(
            repository.countAll(
                searchParams.textSearched,
                searchParams.visibilitySearched,
            ),
            repository.findAll(
                searchParams.textSearched,
                searchParams.visibilitySearched,
                pageable.limit,
                pageable.offset,
            ).map(mapper::toModel).collectList()
        ).map {
            PageModel(pageable, it.t1, it.t2)
        }
    }

    override fun findWithLimit(limit: Int, searchParams: UserSearchParamModel): Flux<UserModel> {
        return repository.findWithLimit(
            searchParams.textSearched,
            searchParams.visibilitySearched,
            limit,
        ).map(mapper::toModel)
    }

    override fun findById(id: UUID, visibilitySearched: Boolean?): Mono<UserModel> {
        return repository.findById(id, visibilitySearched)
            .map(mapper::toModel)
            .switchIfEmpty(Mono.empty())
    }

    override fun findByOidcId(oidcId: UUID, visibilitySearched: Boolean?): Mono<CurrentUserModel> {
        return repository.findByOidcId(oidcId, visibilitySearched)
            .map(currentUserMapper::toModel)
            .switchIfEmpty(Mono.empty())
    }

    override fun findByEmail(email: String, visibilitySearched: Boolean?): Flux<CurrentUserModel> {
        return repository.findByEmail(email, visibilitySearched)
            .map(currentUserMapper::toModel)
    }

    override fun findServiceAccount(): Mono<CurrentUserModel> = repository.findServiceAccount().map(currentUserMapper::toModel)

    override fun findByRoleLevel(roleLevel: Int, visibilitySearched: Boolean?): Flux<UserModel> {
        return repository.findByRoleLevel(roleLevel, visibilitySearched).map(mapper::toModel)
    }

    override fun findUserIdsOlderThanLastLogin(dateThreshold: LocalDate): Flux<UUID> {
        return repository.findUserIdsOlderThanLastLogin(dateThreshold)
    }

    override fun create(element: UserModel): Mono<UserModel> = save(element)
    override fun update(element: UserModel): Mono<UserModel> = save(element)
    private fun save(element: UserModel): Mono<UserModel> = repository.save(mapper.toEntity(element)).map(mapper::toModel)
    override fun deleteById(id: UUID): Mono<Void> = repository.deleteById(id)
}
