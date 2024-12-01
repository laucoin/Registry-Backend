package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_USER_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferencesEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.ONLY_VISIBLE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import java.util.UUID
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
interface IPreferencesEntityRepository: ReactiveCrudRepository<PreferencesEntity, UUID> {
    @Query(
        """
        SELECT t.*, $SELECT_CREATOR, $SELECT_LAST_EDITOR FROM $PREFERENCE_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $ONLY_VISIBLE_CLAUSE AND t.$PREFERENCE_USER_ID = :userId
        """
    )
    fun findByUserId(userId: UUID, onlyVisible: Boolean): Mono<PreferencesEntity>
}
