package fr.laucoin.registry.backend.domain.helper

import fr.laucoin.registry.backend.domain.model.GenericModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * Objectif : Garantit qu'un utilisateur n'est pas le dernier administrateur permanent d'un projet.
 * Périmètre : Verrouille puis compte les profils de niveau 0 de l'utilisateur, au plus sur un projet donné.
 * Limites : Ne modifie aucun profil ; l'appelant décide de l'opération protégée.
 */
@Component
class LastAdministratorHelper(private val port: IProjectProfilePort) {
	private val log = LoggerFactory.getLogger(LastAdministratorHelper::class.java)

	fun <T : GenericModel> validateNotLastProjectRoleLevel0(
		userId: UUID,
		projectId: UUID?,
		result: T,
		error: String
	): Mono<T> {
		return port.lockLevel0ProjectProfilesByUserId(userId)
			.then(Mono.defer { port.findLevel0ProjectProfileRoleByUserId(userId, isVisible = true).collectList() })
			.map { counts -> counts.filter { c -> projectId == null || c.project!!.id == projectId } }
			.handle { it, handle ->
				val projects = it.filter { p -> (p.level0 ?: 0) <= 1 }
				if (projects.isNotEmpty()) {
					log.warn("The user {} is the last administrator of {} project(s)", userId, it.size)
					handle.error(RegistryException(CONFLICT, error, arrayListOf(projects.first().project!!.name)))
				} else handle.next(result)
			}
	}
}
