package fr.laucoin.registry.backend.domain.service.core.impl

import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.ParticipantFeatureModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.domain.service.core.IParticipantSearchService
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

/**
 * Objectif : Recherche les utilisateurs et les groupes proposés lors de la saisie d'un participant.
 * Périmètre : Applique le filtre de visibilité et la limite de résultats configurée pour chaque recherche.
 * Limites : Ne crée ni ne lie aucun participant ; ne renvoie que des éléments visibles.
 */
@Service
class ParticipantSearchService(
	private val userPort: IUserPort,
	private val groupPort: IGroupPort,
	private val feature: ParticipantFeatureModel,
) : IParticipantSearchService {
	override fun searchUsersByText(projectId: UUID, query: String?): Flux<UserModel> {
		return userPort.findWithLimit(
			feature.searched.maxUserResult,
			UserSearchParamModel(query, isVisible = true).apply { memberOfProjectId = projectId },
		)
	}

	override fun searchGroupsByText(projectId: UUID, query: String?): Flux<GroupModel> {
		return groupPort.findWithLimit(
			feature.searched.maxGroupResult,
			projectId,
			GroupSearchParamModel(query, isVisible = true),
		)
	}
}
