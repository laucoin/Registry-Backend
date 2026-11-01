package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.port.IPreferencesPort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.PreferencesEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IPreferencesEntityRepository
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * Objectif : Adaptateur Postgres du port des préférences.
 * Périmètre : Délègue au repository d'entités et mappe les entités vers les modèles du domaine.
 * Limites : Ne porte aucune règle métier.
 */
@Service
class PreferenceModelPostgresRepository(
	private val repository: IPreferencesEntityRepository,
	private val mapper: PreferencesEntityMapper,
): IPreferencesPort {
	override fun findByUserId(userId: UUID, isVisible: Boolean?): Mono<PreferencesModel> {
		return repository.findByUserId(userId, isVisible).map(mapper::toModel)
	}

	override fun save(preference: PreferencesModel): Mono<PreferencesModel> {
		return repository.save(mapper.toEntity(preference)).map(mapper::toModel)
	}
}
