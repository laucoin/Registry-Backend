package fr.laucoin.registry.backend.domain.service.core.impl

import fr.laucoin.registry.backend.domain.model.ProfileFeatureModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.domain.service.core.IProjectProfileSearchService
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

/**
 * Objectif : Recherche les utilisateurs proposés lors de l'invitation d'un profil sur un projet.
 * Périmètre : Applique le filtre de visibilité et la limite de résultats configurée.
 * Limites : N'invite ni ne crée aucun profil ; ne renvoie que des utilisateurs visibles.
 */
@Service
class ProjectProfileSearchService(
	private val userPort: IUserPort,
	private val feature: ProfileFeatureModel,
) : IProjectProfileSearchService {
	override fun searchUsers(query: String?): Flux<UserModel> {
		return userPort.findWithLimit(feature.searched.maxUserResult, UserSearchParamModel(query, isVisible = true))
	}
}
