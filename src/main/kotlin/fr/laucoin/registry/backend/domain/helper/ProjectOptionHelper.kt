package fr.laucoin.registry.backend.domain.helper

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ACTIVITY
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import org.springframework.http.HttpStatus.FORBIDDEN
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * Objectif : Refuse une opération qui sollicite une option de projet que le projet n'a pas activée.
 * Périmètre : Vérifie l'autorité d'option (activité, véhicule…) de l'utilisateur courant sur le projet demandé.
 * Limites : Ne vérifie aucune autre permission ; l'autorisation de l'endpoint reste portée par le contrat.
 */
object ProjectOptionHelper {
	fun <T : Any> Mono<T>.requireProjectOption(
		currentUser: CurrentUserModel,
		projectId: UUID,
		option: String,
		isOptionRequested: Boolean,
	): Mono<T> {
		return if (isOptionRequested && !currentUser.hasAuthority(projectId, option)) {
			Mono.error(RegistryException(status = FORBIDDEN, code = NOT_ENOUGH_PERMISSION))
		} else this
	}

	fun <T : Any> Mono<T>.requireActivityOption(
		currentUser: CurrentUserModel,
		projectId: UUID,
		isActivityRequested: Boolean,
	): Mono<T> = requireProjectOption(currentUser, projectId, REGISTRY_PROJECT_OPTION_ACTIVITY, isActivityRequested)
}
