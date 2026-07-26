package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.enumeration.UserSortFieldEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

interface IUserPort {
	fun findById(id: UUID, visibilitySearched: Boolean?): Mono<UserModel>
	fun findPage(
		pageable: PageableModel,
		searchParams: UserSearchParamModel,
		sort: List<SortModel<UserSortFieldEnum>> = emptyList(),
	): Mono<PageModel<UserModel>>

	fun findWithLimit(limit: Int, searchParams: UserSearchParamModel): Flux<UserModel>
	fun findByOidcId(oidcId: UUID, visibilitySearched: Boolean?): Mono<CurrentUserModel>
	fun findByEmail(email: String, visibilitySearched: Boolean?): Flux<CurrentUserModel>
	fun findServiceAccount(): Mono<CurrentUserModel>
	fun findByRoleLevel(roleLevel: Int, visibilitySearched: Boolean?): Flux<UserModel>
	fun findUserIdsOlderThanLastLogin(dateThreshold: LocalDate): Flux<UUID>
	fun findLightUserIdsOlderThanCreation(dateThreshold: LocalDate): Flux<UUID>
	fun create(element: UserModel): Mono<UserModel>
	fun update(element: UserModel): Mono<UserModel>
	fun deleteById(id: UUID): Mono<Unit>
}
