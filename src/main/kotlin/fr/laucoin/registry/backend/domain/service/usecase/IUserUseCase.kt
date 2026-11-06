package fr.laucoin.registry.backend.domain.service.usecase

import reactor.core.publisher.Flux
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import java.util.UUID
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat de cas d'usage de l'ouverture d'un compte utilisateur.
 * Périmètre : Déclare la création d'un utilisateur à sa première connexion, avec ses préférences par défaut.
 * Limites : Contrat uniquement ; les autres opérations sur les utilisateurs restent dans le service des utilisateurs.
 */
interface IUserUseCase {
	fun createUser(oidcId: UUID, email: String, firstName: String?, lastName: String?): Mono<CurrentUserModel>

	fun findUsersPage(pageable: PageableModel, searchParams: UserSearchParamModel): Mono<PageModel<UserModel>>
	fun findUserById(id: UUID, isVisible: Boolean?): Mono<UserModel>
	fun findUserByOidcId(id: UUID, isVisible: Boolean?): Mono<CurrentUserModel>
	fun findUserByEmail(email: String, isVisible: Boolean?): Flux<CurrentUserModel>
	fun assignableUserRoles(currentUser: CurrentUserModel): Flux<String>
	fun updateUserIfPersonalDataChanged(
		user: CurrentUserModel,
		email: String,
		firstName: String?,
		lastName: String?,
	): Mono<CurrentUserModel>
	fun linkUserToOidcId(user: CurrentUserModel, oidcId: UUID): Mono<CurrentUserModel>
	fun recordLoginByOidcId(oidcId: UUID): Mono<Void>
	fun updateUserRoleById(currentUser: CurrentUserModel, id: UUID, role: String?): Mono<UserModel>
	fun blockUserById(currentUser: CurrentUserModel, id: UUID): Mono<UserModel>
	fun unblockUserById(currentUser: CurrentUserModel, id: UUID): Mono<UserModel>
	fun deleteUserById(currentUser: CurrentUserModel, id: UUID): Mono<Unit>
	fun deleteCurrentUser(currentUser: CurrentUserModel): Mono<Unit>
}
