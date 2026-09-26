package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.model.RoleModel
import reactor.core.publisher.Flux

/**
 * Persistence port exposing the platform's assignable roles, at User level and at Project level.
 * Implemented by the jOOQ Postgres adapter; the domain only depends on this contract.
 */
interface IRolePort {
	fun findUserRoles(): Flux<RoleModel>
	fun findProjectRoles(): Flux<RoleModel>
}
