package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.service.core.IPreferencesService
import fr.laucoin.registry.backend.domain.service.core.IUserService
import fr.laucoin.registry.backend.domain.service.usecase.IUserUseCase
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono

/**
 * Objectif : Ouvre le compte d'un utilisateur et ses préférences par défaut dans une même transaction.
 * Périmètre : Enchaîne la création de l'utilisateur et le chargement de ses préférences.
 * Limites : Ne porte aucune règle propre à l'utilisateur ni aux préférences ; elles restent dans leurs services.
 */
@Service
class UserUseCase(
	private val userService: IUserService,
	private val preferencesService: IPreferencesService,
	private val transactionalOperator: TransactionalOperator,
) : IUserUseCase {
	override fun createUser(
		oidcId: UUID,
		email: String,
		firstName: String?,
		lastName: String?,
	): Mono<CurrentUserModel> {
		return userService.createUser(oidcId, email, firstName, lastName)
			.flatMap(::withPreferences)
			.`as`(transactionalOperator::transactional)
	}

	private fun withPreferences(user: CurrentUserModel): Mono<CurrentUserModel> {
		return preferencesService.findByUser(user)
			.map { user.apply { preferences = it } }
	}
}
