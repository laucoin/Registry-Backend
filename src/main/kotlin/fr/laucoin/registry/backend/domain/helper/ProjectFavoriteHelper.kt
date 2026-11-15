package fr.laucoin.registry.backend.domain.helper

import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.port.IProjectFavoritePort
import java.util.UUID
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * Objectif : Gère les projets favoris d'un utilisateur et renseigne leur statut favori.
 * Périmètre : Ajoute et retire un favori, et marque en une seule requête les projets lus pour l'utilisateur.
 * Limites : Ne vérifie pas l'accès au projet ; l'appelant s'en charge avant d'ajouter un favori.
 */
@Component
class ProjectFavoriteHelper(private val port: IProjectFavoritePort) {
	fun add(userId: UUID, projectId: UUID): Mono<Unit> = port.add(userId, projectId)

	fun remove(userId: UUID, projectId: UUID): Mono<Unit> = port.remove(userId, projectId)

	fun markFavorites(userId: UUID, projects: List<ProjectModel>): Mono<Unit> {
		val ids = projects.mapNotNull { it.id }.distinct()
		if (ids.isEmpty()) return Mono.just(Unit)

		return port.findFavoriteProjectIds(userId, ids)
			.collectList()
			.map { favorites -> projects.forEach { it.favorite = it.id in favorites } }
	}
}
