package fr.laucoin.registry.backend.domain.service.core.impl

import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.domain.service.core.IProjectProfileSearchService
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

/**
 * Objectif : Recherche l'utilisateur à inviter sur un projet à partir de son adresse email exacte.
 * Périmètre : Renvoie au plus l'utilisateur visible portant exactement cet email, sans tenir compte de la casse.
 * Limites : N'invite ni ne crée aucun profil ; ne propose aucune recherche partielle, pour ne pas exposer l'annuaire de la plateforme.
 */
@Service
class ProjectProfileSearchService(private val userPort: IUserPort) : IProjectProfileSearchService {
	override fun searchUsers(query: String?): Flux<UserModel> {
		val email = query?.trim()
		if (email.isNullOrEmpty()) return Flux.empty()

		return userPort.findByEmails(listOf(email), isVisible = true)
	}
}
