package fr.laucoin.registry.backend.domain.service.usecase.impl

import reactor.core.publisher.Flux
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.service.core.IPreferencesService
import fr.laucoin.registry.backend.domain.service.core.IUserService
import fr.laucoin.registry.backend.domain.service.usecase.IUserUseCase
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono

/**
 * Objectif : Ouvre le compte d'un utilisateur et ses préférences par défaut dans une même transaction.
 * Périmètre : Enchaîne la création de l'utilisateur et le chargement de ses préférences.
 * Limites : Ne porte aucune règle propre à l'utilisateur ni aux préférences ; elles restent dans leurs services.
 */
@Service
class UserUseCase(
	private val userService: IUserService,
	private val preferencesService: IPreferencesService,
	private val transactionalOperator: TransactionalOperator,
) : IUserUseCase {
	override fun createUser(
		oidcId: UUID,
		email: String,
		firstName: String?,
		lastName: String?,
	): Mono<CurrentUserModel> {
		return userService.createUser(oidcId, email, firstName, lastName)
			.flatMap(::withPreferences)
			.`as`(transactionalOperator::transactional)
	}

	private fun withPreferences(user: CurrentUserModel): Mono<CurrentUserModel> {
		return preferencesService.findByUser(user)
			.map { user.apply { preferences = it } }
	}

	override fun findUsersPage(
		pageable: PageableModel,
		searchParams: UserSearchParamModel,
	): Mono<PageModel<UserModel>> {
		return userService.findUsersPage(pageable, searchParams)
	}

	override fun findUserById(id: UUID, isVisible: Boolean?): Mono<UserModel> {
		return userService.findUserById(id, isVisible)
	}

	override fun findUserByOidcId(id: UUID, isVisible: Boolean?): Mono<CurrentUserModel> {
		return userService.findUserByOidcId(id, isVisible)
	}

	override fun findUserByEmail(email: String, isVisible: Boolean?): Flux<CurrentUserModel> {
		return userService.findUserByEmail(email, isVisible)
	}

	override fun assignableUserRoles(currentUser: CurrentUserModel): Flux<String> {
		return userService.assignableUserRoles(currentUser)
	}

	override fun updateUserIfPersonalDataChanged(
		user: CurrentUserModel,
		email: String,
		firstName: String?,
		lastName: String?,
	): Mono<CurrentUserModel> {
		return userService.updateUserIfPersonalDataChanged(user, email, firstName, lastName)
	}

	override fun linkUserToOidcId(user: CurrentUserModel, oidcId: UUID): Mono<CurrentUserModel> {
		return userService.linkUserToOidcId(user, oidcId)
	}

	override fun recordLoginByOidcId(oidcId: UUID): Mono<Void> {
		return userService.recordLoginByOidcId(oidcId)
	}

	override fun updateUserRoleById(currentUser: CurrentUserModel, id: UUID, role: String?): Mono<UserModel> {
		return userService.updateUserRoleById(currentUser, id, role)
	}

	override fun blockUserById(currentUser: CurrentUserModel, id: UUID): Mono<UserModel> {
		return userService.blockUserById(currentUser, id)
	}

	override fun unblockUserById(currentUser: CurrentUserModel, id: UUID): Mono<UserModel> {
		return userService.unblockUserById(currentUser, id)
	}

	override fun deleteUserById(currentUser: CurrentUserModel, id: UUID): Mono<Unit> {
		return userService.deleteUserById(currentUser, id)
	}

	override fun deleteCurrentUser(currentUser: CurrentUserModel): Mono<Unit> {
		return userService.deleteCurrentUser(currentUser)
	}
}
