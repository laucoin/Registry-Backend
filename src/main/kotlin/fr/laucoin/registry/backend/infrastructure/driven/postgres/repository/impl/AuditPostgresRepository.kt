package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.AuditModel
import fr.laucoin.registry.backend.domain.port.IAuditPort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.audit.AuditFields.AUDIT_TABLE
import java.util.UUID
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * Objectif : Adaptateur Postgres du port d'écriture de la trace d'audit.
 * Périmètre : Insère un événement d'audit dans la table d'audit, valeurs tronquées à la taille des colonnes.
 * Limites : Insertion seule ; ne porte aucune règle métier et ne lit jamais la table.
 */
@Service
class AuditPostgresRepository(
	private val databaseClient: DatabaseClient,
): IAuditPort {
	override fun record(audit: AuditModel): Mono<Unit> {
		return databaseClient.sql(INSERT_AUDIT)
			.bind("action", audit.action.name)
			.bind("resourceType", audit.resourceType.name)
			.bindNullable("resourceId", audit.resourceId, UUID::class.java)
			.bindNullable("projectId", audit.projectId, UUID::class.java)
			.bindNullable("actorUserId", audit.actorUserId, UUID::class.java)
			.bind("outcome", audit.outcome.name)
			.bindNullable("clientIp", audit.client.ip?.take(IP_MAX), String::class.java)
			.bindNullable("userAgent", audit.client.userAgent?.take(USER_AGENT_MAX), String::class.java)
			.bind("occurredAt", audit.occurredAt.toOffsetDateTime())
			.fetch()
			.rowsUpdated()
			.thenReturn(Unit)
	}

	private fun <T : Any> GenericExecuteSpec.bindNullable(name: String, value: T?, type: Class<T>): GenericExecuteSpec {
		return if (value == null) bindNull(name, type) else bind(name, value)
	}

	private companion object {
		const val IP_MAX = 45
		const val USER_AGENT_MAX = 512

		const val INSERT_AUDIT = """
			INSERT INTO $AUDIT_TABLE (action, resource_type, resource_id, project_id, actor_user_id, outcome, client_ip, user_agent, occurred_at)
			VALUES (:action, :resourceType, :resourceId, :projectId, :actorUserId, :outcome, :clientIp, :userAgent, :occurredAt)
		"""
	}
}
