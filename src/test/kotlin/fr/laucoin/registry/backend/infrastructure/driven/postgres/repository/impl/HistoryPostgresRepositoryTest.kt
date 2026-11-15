package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.port.IAlertHistoryPort
import fr.laucoin.registry.backend.domain.port.ICommunicationHistoryPort
import fr.laucoin.registry.backend.domain.port.IMovementHistoryPort
import fr.laucoin.registry.backend.test.ModelExt.defaultAlertId
import fr.laucoin.registry.backend.test.ModelExt.defaultCommunicationId
import fr.laucoin.registry.backend.test.ModelExt.defaultMovementId
import fr.laucoin.registry.backend.test.TestContext
import java.util.UUID
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.test.StepVerifier

class HistoryPostgresRepositoryTest : TestContext() {
	@Autowired
	private lateinit var alertHistoryPort: IAlertHistoryPort

	@Autowired
	private lateinit var communicationHistoryPort: ICommunicationHistoryPort

	@Autowired
	private lateinit var movementHistoryPort: IMovementHistoryPort

	@Autowired
	private lateinit var databaseClient: DatabaseClient

	@Autowired
	private lateinit var transactionalOperator: TransactionalOperator

	private fun count(sql: String, id: UUID) = databaseClient.sql(sql)
		.bind("id", id)
		.map { row -> row.get(0, Long::class.javaObjectType)!! }
		.one()

	@Test
	fun `Should archiveAlert increment the version at each call`() {
		// Arrange
		val sql = "SELECT COALESCE(MAX(version), 0) FROM tb_alert_history WHERE alert_id = :id"
		val before = count(sql, defaultAlertId).block()!!

		// Act
		val result = alertHistoryPort.archiveAlert(defaultAlertId)
			.then(alertHistoryPort.archiveAlert(defaultAlertId))
			.then(count(sql, defaultAlertId))
			.`as`(transactionalOperator::transactional)

		// Assert
		StepVerifier.create(result).expectNext(before + 2).verifyComplete()
	}

	@Test
	fun `Should archiveCommunication copy the current row`() {
		// Arrange
		val sql = "SELECT COUNT(*) FROM tb_communication_history WHERE communication_id = :id"
		val before = count(sql, defaultCommunicationId).block()!!

		// Act
		val result = communicationHistoryPort.archiveCommunication(defaultCommunicationId)
			.then(count(sql, defaultCommunicationId))
			.`as`(transactionalOperator::transactional)

		// Assert
		StepVerifier.create(result).expectNext(before + 1).verifyComplete()
	}

	@Test
	fun `Should archiveMovement copy the row and its content`() {
		// Arrange
		val sql = "SELECT COUNT(*) FROM tb_movement_content_history ch " +
			"JOIN tb_movement_history h ON h.id = ch.movement_history_id WHERE h.movement_id = :id"
		val before = count(sql, defaultMovementId).block()!!

		// Act
		val result = movementHistoryPort.archiveMovement(defaultMovementId)
			.then(count(sql, defaultMovementId))
			.`as`(transactionalOperator::transactional)

		// Assert
		StepVerifier.create(result).expectNextMatches { it > before }.verifyComplete()
	}

	@Test
	fun `Should archive nothing when the row does not exist`() {
		// Arrange
		val id = UUID.randomUUID()

		// Act
		val result = alertHistoryPort.archiveAlert(id)
			.then(count("SELECT COUNT(*) FROM tb_alert_history WHERE alert_id = :id", id))

		// Assert
		StepVerifier.create(result).expectNext(0L).verifyComplete()
	}
}
