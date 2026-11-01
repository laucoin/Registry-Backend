package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum.ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum.ALERT
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum.COMMUNICATION
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum.GROUP
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum.MOVEMENT
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum.PARTICIPANT
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum.PROJECT
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum.PROJECT_PROFILE
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum.VEHICLE
import fr.laucoin.registry.backend.domain.port.IRowLockPort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.activity.ActivityFields.ACTIVITY_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertFields.ALERT_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.communication.CommunicationFields.COMMUNICATION_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupFields.GROUP_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementFields.MOVEMENT_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.vehicle.VehicleFields.VEHICLE_TABLE
import java.util.UUID
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * Objectif : Adaptateur Postgres du port de verrou de ligne.
 * Périmètre : Émet `SELECT ... FOR UPDATE` sur la table de la ressource demandée, avec l'id comme paramètre lié.
 * Limites : Les noms de tables viennent de constantes via l'enum des ressources, jamais de la requête.
 */
@Service
class RowLockPostgresRepository(
	private val databaseClient: DatabaseClient,
) : IRowLockPort {
	override fun lock(resource: LockableResourceEnum, id: UUID): Mono<Unit> {
		return databaseClient.sql("SELECT t.$ID FROM ${tableOf(resource)} t WHERE t.$ID = :id FOR UPDATE")
			.bind("id", id)
			.map { row -> row.get(0, UUID::class.java)!! }
			.all()
			.then(Mono.just(Unit))
	}

	private fun tableOf(resource: LockableResourceEnum): String = when (resource) {
		ACTIVITY -> ACTIVITY_TABLE
		ALERT -> ALERT_TABLE
		COMMUNICATION -> COMMUNICATION_TABLE
		GROUP -> GROUP_TABLE
		MOVEMENT -> MOVEMENT_TABLE
		PARTICIPANT -> PARTICIPANT_TABLE
		PROJECT -> PROJECT_TABLE
		PROJECT_PROFILE -> PROJECT_PROFILE_TABLE
		VEHICLE -> VEHICLE_TABLE
	}
}
