package fr.laucoin.registry.backend.domain.helper

import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.port.IProjectFavoritePort
import java.util.UUID
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * Objectif : Renseigne le statut favori du projet porté par des profils de projet.
 * Périmètre : Interroge les favoris de l'utilisateur pour un profil ou une page de profils et marque leur projet.
 * Limites : Ne modifie aucun favori ; ne charge que les favoris des projets déjà présents dans les profils.
 */
@Component
class ProjectProfileFavoriteHelper(private val favoritePort: IProjectFavoritePort) {
	fun withFavorite(userId: UUID, profile: ProjectProfileModel): Mono<ProjectProfileModel> {
		val projectId = profile.project?.id ?: return Mono.just(profile)
		return favoritePort.findFavoriteProjectIds(userId, listOf(projectId))
			.hasElements()
			.map { profile.apply { project?.favorite = it } }
	}

	fun withFavorites(userId: UUID, page: PageModel<ProjectProfileModel>): Mono<PageModel<ProjectProfileModel>> {
		return favoritePort.findFavoriteProjectIds(userId, page.content.mapNotNull { it.project?.id }.distinct())
			.collectList()
			.map { page.withFavoriteProjects(it.toSet()) }
	}

	private fun PageModel<ProjectProfileModel>.withFavoriteProjects(
		favoriteIds: Set<UUID>,
	): PageModel<ProjectProfileModel> {
		content.forEach { profile -> profile.project?.let { it.favorite = it.id in favoriteIds } }
		return this
	}
}
