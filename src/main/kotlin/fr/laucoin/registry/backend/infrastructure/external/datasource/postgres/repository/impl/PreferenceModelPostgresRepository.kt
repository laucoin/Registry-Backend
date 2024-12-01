package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.repository.IPreferencesModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.PreferencesEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IPreferencesEntityRepository
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class PreferenceModelPostgresRepository(
    private val repository: IPreferencesEntityRepository,
    private val mapper: PreferencesEntityMapper,
): IPreferencesModelRepository {
    override fun findByUserId(userId: UUID, onlyVisible: Boolean): Mono<PreferencesModel> {
        return repository.findByUserId(userId, onlyVisible).map(mapper::toModel)
    }

    override fun save(preference: PreferencesModel): Mono<PreferencesModel> {
        return repository.save(mapper.toEntity(preference)).map(mapper::toModel)
    }
}
