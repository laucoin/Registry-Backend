package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.port.IPreferencesPort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.PreferencesEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.PreferencesJooqRepository
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * [IPreferencesPort] implementation: translates every call to [PreferencesJooqRepository] and maps
 * the entity ↔ [PreferencesModel] via [PreferencesEntityMapper]. No business logic of its own.
 */
@Service
class PreferenceModelPostgresRepository(
	private val repository: PreferencesJooqRepository,
	private val mapper: PreferencesEntityMapper,
): IPreferencesPort {
	override fun findByUserId(userId: UUID, isVisible: Boolean?): Mono<PreferencesModel> {
		return repository.findByUserId(userId, isVisible).map(mapper::toModel)
	}

	override fun save(preference: PreferencesModel): Mono<PreferencesModel> {
		return repository.save(mapper.toEntity(preference)).map(mapper::toModel)
	}
}
