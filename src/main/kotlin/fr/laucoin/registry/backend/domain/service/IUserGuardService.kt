package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.UserModel
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat des garde-fous sur un utilisateur.
 * Périmètre : Déclare les contrôles d'auto-modification, de dernier administrateur et de rôle attribuable.
 * Limites : Contrat uniquement.
 */
interface IUserGuardService {
	fun validateNotCurrentUser(user: UserModel, currentUser: CurrentUserModel, error: String): Mono<UserModel>

	fun validateNotLastRoleLevel0(userToUpdate: UserModel, error: String): Mono<UserModel>

	fun validateNotLastProjectRoleLevel0(user: UserModel, error: String): Mono<UserModel>

	fun validateRole(
		user: UserModel,
		currentUser: CurrentUserModel,
		eligibleRoles: List<String>,
		role: String?,
		error: String,
	): Mono<UserModel>
}
