package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.port.IAlertHistoryPort
import fr.laucoin.registry.backend.domain.port.ICommunicationHistoryPort
import fr.laucoin.registry.backend.domain.port.IMovementHistoryPort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertFields.ALERT_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.communication.CommunicationFields.COMMUNICATION_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.history.HistoryFields.ALERT_HISTORY_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.history.HistoryFields.COMMUNICATION_HISTORY_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.history.HistoryFields.MOVEMENT_CONTENT_HISTORY_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.history.HistoryFields.MOVEMENT_HISTORY_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementFields.MOVEMENT_TABLE
import java.util.UUID
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * Objectif : Adaptateur Postgres des ports d'historique de l'alerte, de la communication et du mouvement.
 * Périmètre : Copie côté SQL l'état courant de la ligne dans sa table d'historique avec le numéro de version suivant.
 * Limites : Ne porte aucune règle métier ; le verrou de ligne et la transaction sont ceux de l'appelant.
 */
@Service
class HistoryPostgresRepository(
	private val databaseClient: DatabaseClient,
): IAlertHistoryPort, ICommunicationHistoryPort, IMovementHistoryPort {
	override fun archiveAlert(id: UUID): Mono<Unit> = execute(ARCHIVE_ALERT, id)

	override fun archiveCommunication(id: UUID): Mono<Unit> = execute(ARCHIVE_COMMUNICATION, id)

	override fun archiveMovement(id: UUID): Mono<Unit> = execute(ARCHIVE_MOVEMENT, id)

	private fun execute(sql: String, id: UUID): Mono<Unit> {
		return databaseClient.sql(sql)
			.bind("id", id)
			.fetch()
			.rowsUpdated()
			.thenReturn(Unit)
	}

	private companion object {
		const val ARCHIVE_ALERT = """
			INSERT INTO $ALERT_HISTORY_TABLE (alert_id, version, date_time, title, status, project_id, created_date, created_by, last_modified_date, last_modified_by, visible)
			SELECT a.id, (SELECT COALESCE(MAX(h.version), 0) + 1 FROM $ALERT_HISTORY_TABLE h WHERE h.alert_id = a.id),
				a.date_time, a.title, a.status, a.project_id, a.created_date, a.created_by, a.last_modified_date, a.last_modified_by, a.visible
			FROM $ALERT_TABLE a WHERE a.id = :id
		"""

		const val ARCHIVE_COMMUNICATION = """
			INSERT INTO $COMMUNICATION_HISTORY_TABLE (communication_id, version, date_time, movement_id, alert_id, message, project_id, created_date, created_by, last_modified_date, last_modified_by, visible)
			SELECT c.id, (SELECT COALESCE(MAX(h.version), 0) + 1 FROM $COMMUNICATION_HISTORY_TABLE h WHERE h.communication_id = c.id),
				c.date_time, c.movement_id, c.alert_id, c.message, c.project_id, c.created_date, c.created_by, c.last_modified_date, c.last_modified_by, c.visible
			FROM $COMMUNICATION_TABLE c WHERE c.id = :id
		"""

		const val ARCHIVE_MOVEMENT = """
			WITH archived AS (
				INSERT INTO $MOVEMENT_HISTORY_TABLE (movement_id, version, date_time, type, reason, activity_id, project_id, created_date, created_by, last_modified_date, last_modified_by, visible)
				SELECT m.id, (SELECT COALESCE(MAX(h.version), 0) + 1 FROM $MOVEMENT_HISTORY_TABLE h WHERE h.movement_id = m.id),
					m.date_time, m.type, m.reason, m.activity_id, m.project_id, m.created_date, m.created_by, m.last_modified_date, m.last_modified_by, m.visible
				FROM $MOVEMENT_TABLE m WHERE m.id = :id
				RETURNING id
			)
			INSERT INTO $MOVEMENT_CONTENT_HISTORY_TABLE (movement_history_id, participant_id, vehicle_id, pool_name)
			SELECT archived.id, c.participant_id, c.vehicle_id, c.pool_name
			FROM archived JOIN $MOVEMENT_CONTENT_TABLE c ON c.movement_id = :id
		"""
	}
}
