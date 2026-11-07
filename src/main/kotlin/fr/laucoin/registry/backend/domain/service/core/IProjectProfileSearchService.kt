package fr.laucoin.registry.backend.domain.service.core

import fr.laucoin.registry.backend.domain.model.UserModel
import reactor.core.publisher.Flux

/**
 * Objectif : Contrat de recherche des utilisateurs à inviter sur un projet.
 * Périmètre : Déclare la recherche textuelle bornée des utilisateurs visibles.
 * Limites : Contrat uniquement ; la limite de résultats vient de la configuration.
 */
interface IProjectProfileSearchService {
	fun searchUsers(query: String?): Flux<UserModel>
}
