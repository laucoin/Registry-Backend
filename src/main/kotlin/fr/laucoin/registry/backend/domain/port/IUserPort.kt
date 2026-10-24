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
 * Objectif : Port driven donnant au domaine l'accès à la persistance de l'utilisateur.
 * Périmètre : Déclare les lectures, recherches et écritures de l'utilisateur dont dépendent les services.
 * Limites : Contrat uniquement ; l'adaptateur Postgres porte le SQL, les entités et le mapping.
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
