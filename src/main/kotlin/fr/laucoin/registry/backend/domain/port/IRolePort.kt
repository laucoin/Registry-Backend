package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.model.RoleModel
import reactor.core.publisher.Flux

interface IRolePort {
	fun findUserRoles(): Flux<RoleModel>
	fun findProjectRoles(): Flux<RoleModel>
}
