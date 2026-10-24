package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ACTIVITY
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.GenericModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.service.impl.LoggerService
import org.springframework.http.HttpStatus.FORBIDDEN
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * Objectif : Classe de base des services, avec les utilitaires qu'ils partagent.
 * Périmètre : Fournit les mises à jour de visibilité, la constante de concurrence des purges et la garde de l'option d'activité.
 * Limites : Ne porte aucun cas d'usage propre.
 */
open class GenericService : LoggerService() {
	fun <T : GenericModel> Mono<T>.updateVisibility(visibility: Boolean): Mono<T> {
		return this.map { it.apply { visible = visibility } }
	}

	fun <T : Any> Mono<T>.requireActivityOption(
		currentUser: CurrentUserModel,
		projectId: UUID,
		isActivityRequested: Boolean,
	): Mono<T> {
		return if (isActivityRequested && !currentUser.hasAuthority(projectId, REGISTRY_PROJECT_OPTION_ACTIVITY)) {
			Mono.error(RegistryException(status = FORBIDDEN, code = NOT_ENOUGH_PERMISSION))
		} else this
	}

	companion object {
		const val PURGE_DELETE_CONCURRENCY = 8
	}
}
