package fr.laucoin.registry.backend.domain.service.usecase

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import java.util.UUID
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat de cas d'usage de l'ouverture d'un compte utilisateur.
 * Périmètre : Déclare la création d'un utilisateur à sa première connexion, avec ses préférences par défaut.
 * Limites : Contrat uniquement ; les autres opérations sur les utilisateurs restent dans le service des utilisateurs.
 */
interface IUserUseCase {
	fun createUser(oidcId: UUID, email: String, firstName: String?, lastName: String?): Mono<CurrentUserModel>
}
