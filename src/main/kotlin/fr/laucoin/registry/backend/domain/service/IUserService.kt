package fr.laucoin.registry.backend.domain.service

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

interface IUserService {
	fun findUsersPage(
		pageable: PageableModel,
		searchParams: UserSearchParamModel,
		sort: List<SortModel<UserSortFieldEnum>> = emptyList(),
	): Mono<PageModel<UserModel>>

	fun findUserById(id: UUID, visibilitySearched: Boolean?): Mono<UserModel>
	fun findUserByOidcId(id: UUID, visibilitySearched: Boolean?): Mono<CurrentUserModel>
	fun findUserByEmail(email: String, visibilitySearched: Boolean?): Flux<CurrentUserModel>
	fun serviceAccount(): UserModel
	fun assignableUserRoles(currentUser: CurrentUserModel): Flux<String>
	fun createUser(oidcId: UUID, email: String, firstName: String?, lastName: String?): Mono<CurrentUserModel>
	fun linkUser(
		user: CurrentUserModel,
		oidcId: UUID,
		email: String,
		firstName: String?,
		lastName: String?
	): Mono<CurrentUserModel>

	fun findOrCreateInvitedUser(email: String, inviter: CurrentUserModel): Mono<UserModel>
	fun updateUserIfPersonalDataChanged(
		user: CurrentUserModel,
		email: String,
		firstName: String?,
		lastName: String?
	): Mono<CurrentUserModel>

	fun updateUserRoleById(currentUser: CurrentUserModel, id: UUID, role: String?): Mono<UserModel>
	fun blockUserById(currentUser: CurrentUserModel, id: UUID): Mono<UserModel>
	fun unblockUserById(currentUser: CurrentUserModel, id: UUID): Mono<UserModel>
	fun deleteUserById(currentUser: CurrentUserModel, id: UUID): Mono<Unit>
	fun deleteCurrentUser(currentUser: CurrentUserModel): Mono<Unit>
	fun purgeUsersIfNecessary(dateThreshold: LocalDate, dryRun: Boolean): Flux<UUID>
	fun purgeLightUsersIfNecessary(dateThreshold: LocalDate, dryRun: Boolean): Flux<UUID>
}
