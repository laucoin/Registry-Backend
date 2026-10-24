package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import java.util.UUID
import reactor.core.publisher.Mono

/**
 * Objectif : Port driven donnant au domaine l'accès aux préférences de l'utilisateur.
 * Périmètre : Lit et enregistre les préférences d'un utilisateur.
 * Limites : Contrat uniquement ; l'adaptateur Postgres porte le SQL et le mapping.
 */
interface IPreferencesPort {
	fun findByUserId(userId: UUID, isVisible: Boolean?): Mono<PreferencesModel>
	fun save(preference: PreferencesModel): Mono<PreferencesModel>
}
