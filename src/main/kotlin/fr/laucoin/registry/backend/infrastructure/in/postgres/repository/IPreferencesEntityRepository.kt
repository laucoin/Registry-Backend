package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository

import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.preference.PreferenceFields.PREFERENCE_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.preference.PreferenceFields.PREFERENCE_USER_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.preference.PreferencesEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.VISIBLE_CLAUSE
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
        WHERE t.$PREFERENCE_USER_ID = :userId AND $VISIBLE_CLAUSE
        """
	)
	fun findByUserId(userId: UUID, visibilitySearched: Boolean?): Mono<PreferencesEntity>
}
