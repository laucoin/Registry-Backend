package fr.laucoin.registry.backend.domain.helper

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ACTIVITY
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import org.springframework.http.HttpStatus.FORBIDDEN
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * Objectif : Refuse une lecture qui demande des données d'activité sans l'option d'activité du projet.
 * Périmètre : Vérifie l'autorité d'option d'activité de l'utilisateur courant sur le projet demandé.
 * Limites : Ne vérifie aucune autre permission ; l'autorisation de l'endpoint reste portée par le contrat.
 */
object ActivityOptionHelper {
	fun <T : Any> Mono<T>.requireActivityOption(
		currentUser: CurrentUserModel,
		projectId: UUID,
		isActivityRequested: Boolean,
	): Mono<T> {
		return if (isActivityRequested && !currentUser.hasAuthority(projectId, REGISTRY_PROJECT_OPTION_ACTIVITY)) {
			Mono.error(RegistryException(status = FORBIDDEN, code = NOT_ENOUGH_PERMISSION))
		} else this
	}
}
