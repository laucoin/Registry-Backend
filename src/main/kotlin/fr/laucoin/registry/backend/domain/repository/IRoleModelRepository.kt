package fr.laucoin.registry.backend.domain.repository

import fr.laucoin.registry.backend.domain.model.RoleModel
import reactor.core.publisher.Flux

interface IRoleModelRepository {
    fun findUserRoles(): Flux<RoleModel>
    fun findEventRoles(): Flux<RoleModel>
}
