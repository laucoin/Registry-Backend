package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.preference.PreferenceFields.PREFERENCE_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.preference.PreferenceFields.PREFERENCE_USER_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.preference.PreferencesEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.VISIBLE_CLAUSE
import java.util.UUID
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

/**
 * Purpose: Reactive entity repository of the preferences table.
 * Scope: Holds the parameterized SQL queries (filters, sort, pagination, locks) on preferences.
 * Limits: Used by the Postgres adapter only; returns entities, never domain models.
 */
@Repository
interface IPreferencesEntityRepository: ReactiveCrudRepository<PreferencesEntity, UUID> {
	@Query(
		"""
        SELECT t.*, $SELECT_CREATOR, $SELECT_LAST_EDITOR FROM $PREFERENCE_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE t.$PREFERENCE_USER_ID = :userId AND $VISIBLE_CLAUSE
        """
	)
	fun findByUserId(userId: UUID, isVisible: Boolean?): Mono<PreferencesEntity>
}
