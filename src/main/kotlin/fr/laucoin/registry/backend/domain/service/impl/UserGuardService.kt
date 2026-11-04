package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserGuardService
import fr.laucoin.registry.backend.domain.service.IUserProjectProfileService
import java.util.Objects
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * Objectif : Applique les garde-fous sur un utilisateur modifié.
* Périmètre : Refus de l'auto-modification, de la perte du dernier administrateur (application ou projet) et d'un rôle
 *   non attribuable.
 * Limites : Ne persiste rien ; chaque contrôle échoue par une RegistryException.
 */
@Service
class UserGuardService(
	private val port: IUserPort,
	private val roleService: IRoleService,
	private val userProjectProfileService: IUserProjectProfileService,
) : IUserGuardService, LoggerService() {
	override fun validateNotCurrentUser(user: UserModel, currentUser: CurrentUserModel, error: String): Mono<UserModel> =
		Mono.just(user).handle { found, handle ->
			if (found.id == currentUser.id) {
				log.warn("The user {} is the current user", currentUser.id)
				handle.error(RegistryException(FORBIDDEN, error))
			} else handle.next(found)
		}

	override fun validateNotLastRoleLevel0(userToUpdate: UserModel, error: String): Mono<UserModel> {
		val roleLevel = roleService.getLevelByUserRole(userToUpdate.role)
		if (Objects.isNull(roleLevel) || roleLevel!! > 0) return Mono.just(userToUpdate)

		return port.lockLevel0Users()
			.then(Mono.defer { port.findByRoleLevel(roleLevel = 0, isVisible = true).collectList() })
			.map { administrators -> administrators.any { userToUpdate.id != it.id } }
			.flatMap { hasOtherAdministrator ->
				if (!hasOtherAdministrator) {
					log.warn("The user {} is the last administrator of the application", userToUpdate.id)
					Mono.error(RegistryException(CONFLICT, error))
				} else Mono.just(userToUpdate)
			}
	}

	override fun validateNotLastProjectRoleLevel0(user: UserModel, error: String): Mono<UserModel> =
		userProjectProfileService.validateNotLastProjectRoleLevel0(user.id!!, projectId = null, user, error)

	override fun validateRole(
		user: UserModel,
		currentUser: CurrentUserModel,
		eligibleRoles: List<String>,
		role: String?,
		error: String,
	): Mono<UserModel> =
		Mono.just(user).handle { found, handle ->
			if (Objects.nonNull(role) && !eligibleRoles.contains(role)) {
				log.warn("The role {} is not assignable by the user {}", role, currentUser.id)
				handle.error(RegistryException(FORBIDDEN, error, arrayListOf(role)))
			} else handle.next(found)
		}
}
