package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.RoleModel
import fr.laucoin.registry.backend.domain.repository.IRoleModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.RoleEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IRoleEntityRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class RoleModelPostgresRepository(
    private val repository: IRoleEntityRepository,
    private val mapper: RoleEntityMapper,
): IRoleModelRepository {
    override fun findUserRoles(): Flux<RoleModel> {
        return repository.findUserRoles().map(mapper::toModel)
    }

    override fun findEventRoles(): Flux<RoleModel> {
        return repository.findEventRoles().map(mapper::toModel)
    }
}
