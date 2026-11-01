package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository

import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertFields.ALERT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertFields.ALERT_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertQueries.ALERT_CRITICALITY_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertQueries.ALERT_DATE_IN_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertQueries.ALERT_LAST_COMMUNICATION_JOIN
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertQueries.ALERT_SORT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertQueries.ALERT_STATUS_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertQueries.ALERT_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertQueries.SELECT_ALERT_CRITICALITY
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertQueries.SELECT_ALERT_LAST_COMMUNICATION
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertQueries.SELECT_ALERT_SEARCH
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.communication.CommunicationFields.COMMUNICATION_ALERT_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.communication.CommunicationFields.COMMUNICATION_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.communication.CommunicationFields.COMMUNICATION_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.FULL_COUNT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.LAST_MODIFIER_DATE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.PROJECT_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.PROJECT_JOIN
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.VISIBLE_CLAUSE
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Repository d'entités réactif de la table de l'alerte.
 * Périmètre : Porte les requêtes SQL paramétrées de l'alerte (filtres, tri, pagination, verrous).
 * Limites : Utilisé uniquement par l'adaptateur Postgres ; renvoie des entités, jamais des modèles du domaine.
 */
@Repository
interface IAlertEntityRepository: ReactiveCrudRepository<AlertEntity, UUID> {
	@Query(
		"""
        SELECT t.*, $SELECT_ALERT_SEARCH, $SELECT_ALERT_CRITICALITY, $SELECT_ALERT_LAST_COMMUNICATION, $SELECT_CREATOR, $SELECT_LAST_EDITOR, COUNT(*) OVER() AS $FULL_COUNT
        FROM $ALERT_TABLE t $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN $ALERT_LAST_COMMUNICATION_JOIN
        WHERE $PROJECT_CLAUSE AND $VISIBLE_CLAUSE AND $ALERT_TEXT_SEARCH_CLAUSE AND $ALERT_STATUS_SEARCH_CLAUSE AND $ALERT_CRITICALITY_CLAUSE AND $ALERT_DATE_IN_DATES_RANGE_CLAUSE
        ORDER BY $ALERT_SORT similarity_score DESC, t.$ALERT_DATE_TIME DESC, t.$ID
        LIMIT :limit OFFSET :offset
        """
	)
	fun findAll(
		projectId: UUID,
		query: String?,
		status: List<AlertStatusEnum>?,
		criticality: AlertCriticalityEnum?,
		reinforcedAfterHours: Long,
		highAfterHours: Long,
		isVisible: Boolean?,
		from: ZonedDateTime?,
		to: ZonedDateTime?,
		sortField: String?,
		isDescending: Boolean?,
		limit: Int,
		offset: Long,
	): Flux<AlertEntity>

	@Query(
		"""
        SELECT t.*, $SELECT_ALERT_SEARCH, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $ALERT_TABLE t $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND $VISIBLE_CLAUSE AND $ALERT_TEXT_SEARCH_CLAUSE AND $ALERT_STATUS_SEARCH_CLAUSE AND $ALERT_DATE_IN_DATES_RANGE_CLAUSE
        ORDER BY similarity_score DESC, t.$ALERT_DATE_TIME DESC
        LIMIT :limit
        """
	)
	fun findWithLimit(
		projectId: UUID,
		query: String?,
		status: List<AlertStatusEnum>?,
		isVisible: Boolean?,
		from: ZonedDateTime?,
		to: ZonedDateTime?,
		limit: Int,
	): Flux<AlertEntity>


	@Query(
		"""
        SELECT t.*, $SELECT_ALERT_CRITICALITY, $SELECT_ALERT_LAST_COMMUNICATION, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $ALERT_TABLE t $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN $ALERT_LAST_COMMUNICATION_JOIN
        WHERE $PROJECT_CLAUSE AND $VISIBLE_CLAUSE AND t.$ID = :id
        """
	)
	fun findById(
		projectId: UUID,
		id: UUID,
		isVisible: Boolean?,
		reinforcedAfterHours: Long,
		highAfterHours: Long,
	): Mono<AlertEntity>

	@Query(
		"""
        SELECT t.$ID
        FROM $ALERT_TABLE t
        LEFT JOIN (
            SELECT MAX(tc.$COMMUNICATION_DATE_TIME), tc.$COMMUNICATION_ALERT_ID FROM $COMMUNICATION_TABLE tc
            WHERE tc.$COMMUNICATION_ALERT_ID IS NOT NULL
            GROUP BY tc.$COMMUNICATION_ALERT_ID
        ) lc ON lc.$COMMUNICATION_ALERT_ID = t.$ID
        WHERE (lc.max IS NULL OR lc.max < :dateThreshold) AND t.$LAST_MODIFIER_DATE < :dateThreshold
        """
	)
	fun findOlderThanAndUncommentedSince(dateThreshold: LocalDate): Flux<UUID>
}
