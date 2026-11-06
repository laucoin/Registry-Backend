package fr.laucoin.registry.backend.domain.helper

import fr.laucoin.registry.backend.domain.model.ProjectProfileRoleCountModel
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.nextOrError
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
			.handle { counts, sink -> sink.nextOrError(result, lastAdministratorError(userId, counts, error)) }
	}

	private fun lastAdministratorError(
		userId: UUID,
		counts: List<ProjectProfileRoleCountModel>,
		error: String,
	): RegistryException? {
		val projects = counts.filter { (it.level0 ?: 0) <= 1 }
		if (projects.isEmpty()) return null
		log.warn("The user {} is the last administrator of {} project(s)", userId, counts.size)
		return RegistryException(CONFLICT, error, arrayListOf(projects.first().project!!.name))
	}
}
