package fr.laucoin.registry.backend.domain.service.core

import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.UserModel
import java.util.UUID
import reactor.core.publisher.Flux

/**
 * Objectif : Contrat de recherche des utilisateurs et des groupes à lier à un participant.
 * Périmètre : Déclare la recherche textuelle bornée des utilisateurs du projet et des groupes.
 * Limites : Contrat uniquement ; les limites de résultats viennent de la configuration.
 */
interface IParticipantSearchService {
	fun searchUsersByText(projectId: UUID, query: String?): Flux<UserModel>
	fun searchGroupsByText(projectId: UUID, query: String?): Flux<GroupModel>
}
