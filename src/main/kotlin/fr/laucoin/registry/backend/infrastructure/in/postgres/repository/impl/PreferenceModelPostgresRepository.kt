package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.port.IPreferencesPort
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.PreferencesEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.IPreferencesEntityRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class PreferenceModelPostgresRepository(
	private val repository: IPreferencesEntityRepository,
	private val mapper: PreferencesEntityMapper,
) : IPreferencesPort {
	override fun findByUserId(userId: UUID, visibilitySearched: Boolean?): Mono<PreferencesModel> {
		return repository.findByUserId(userId, visibilitySearched).map(mapper::toModel)
	}

	override fun save(preference: PreferencesModel): Mono<PreferencesModel> {
		return repository.save(mapper.toEntity(preference)).map(mapper::toModel)
	}
}
