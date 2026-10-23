package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import java.time.LocalDate
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Purpose: Driven port giving the domain access to the persisted user.
 * Scope: Declares the reads, searches and writes of user that the services rely on.
 * Limits: Contract only; the Postgres adapter owns SQL, entities and mapping.
 */
interface IUserPort {
	fun findById(id: UUID, isVisible: Boolean?): Mono<UserModel>
	fun findPage(pageable: PageableModel, searchParams: UserSearchParamModel): Mono<PageModel<UserModel>>
	fun findWithLimit(limit: Int, searchParams: UserSearchParamModel): Flux<UserModel>
	fun findByOidcId(oidcId: UUID, isVisible: Boolean?): Mono<CurrentUserModel>
	fun findByEmail(email: String, isVisible: Boolean?): Flux<CurrentUserModel>
	fun findServiceAccount(): Mono<CurrentUserModel>
	fun findByRoleLevel(roleLevel: Int, isVisible: Boolean?): Flux<UserModel>
	fun lockLevel0Users(): Flux<UUID>
	fun findUserIdsOlderThanLastLogin(dateThreshold: LocalDate): Flux<UUID>
	fun create(element: UserModel): Mono<UserModel>
	fun update(element: UserModel): Mono<UserModel>
	fun deleteById(id: UUID): Mono<Unit>
}
