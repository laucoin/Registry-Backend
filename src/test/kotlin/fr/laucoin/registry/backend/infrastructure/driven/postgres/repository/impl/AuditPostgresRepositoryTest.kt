package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.AuditActionEnum
import fr.laucoin.registry.backend.domain.enumeration.AuditOutcomeEnum
import fr.laucoin.registry.backend.domain.enumeration.AuditResourceTypeEnum
import fr.laucoin.registry.backend.domain.model.AuditModel
import fr.laucoin.registry.backend.domain.model.ClientContextModel
import fr.laucoin.registry.backend.domain.port.IAuditPort
import fr.laucoin.registry.backend.test.TestContext
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

class AuditPostgresRepositoryTest : TestContext() {
	@Autowired
	private lateinit var port: IAuditPort

	@Autowired
	private lateinit var databaseClient: DatabaseClient

	private fun audit(
		actor: UUID,
		client: ClientContextModel,
		projectId: UUID? = null,
		occurredAt: ZonedDateTime = ZonedDateTime.parse("2026-01-01T10:00:00Z"),
	) = AuditModel(
		action = AuditActionEnum.EXPORT,
		resourceType = AuditResourceTypeEnum.PARTICIPANT,
		resourceId = UUID.randomUUID(),
		projectId = projectId,
		actorUserId = actor,
		outcome = AuditOutcomeEnum.RATE_LIMITED,
		client = client,
		occurredAt = occurredAt,
	)

	private fun select(actor: UUID) = databaseClient
		.sql("SELECT action, resource_type, outcome, client_ip, user_agent, project_id FROM tb_audit WHERE actor_user_id = :actor")
		.bind("actor", actor)
		.map { row ->
			listOf(
				row.get("action", String::class.java),
				row.get("resource_type", String::class.java),
				row.get("outcome", String::class.java),
				row.get("client_ip", String::class.java),
				row.get("user_agent", String::class.java),
				row.get("project_id", UUID::class.java),
			)
		}
		.one()

	@Test
	fun `Should record persist the event with its client context`() {
		// Arrange
		val actor = UUID.randomUUID()
		val projectId = UUID.randomUUID()

		// Act
		val result = port.record(audit(actor, ClientContextModel("203.0.113.7", "JUnit/1.0"), projectId))
			.then(select(actor))

		// Assert
		StepVerifier.create(result)
			.expectNext(listOf("EXPORT", "PARTICIPANT", "RATE_LIMITED", "203.0.113.7", "JUnit/1.0", projectId))
			.verifyComplete()
	}

	@Test
	fun `Should record persist null client values and no project`() {
		// Arrange
		val actor = UUID.randomUUID()

		// Act
		val result = port.record(audit(actor, ClientContextModel())).then(select(actor))

		// Assert
		StepVerifier.create(result)
			.expectNext(listOf("EXPORT", "PARTICIPANT", "RATE_LIMITED", null, null, null))
			.verifyComplete()
	}

	@Test
	fun `Should record truncate the user agent to the column size`() {
		// Arrange
		val actor = UUID.randomUUID()

		// Act
		val result = port.record(audit(actor, ClientContextModel("::1", "a".repeat(600)))).then(select(actor))

		// Assert
		StepVerifier.create(result.map { (it[4] as String).length }).expectNext(512).verifyComplete()
	}

	@Test
	fun `Should purgeOlderThan count on a dry run then delete only the events older than the threshold`() {
		// Arrange
		val client = ClientContextModel("203.0.113.7", "JUnit")
		val old = audit(UUID.randomUUID(), client, occurredAt = ZonedDateTime.parse("1999-12-31T23:00:00Z"))
		val older = audit(UUID.randomUUID(), client, occurredAt = ZonedDateTime.parse("1998-06-01T10:00:00Z"))
		val kept = audit(UUID.randomUUID(), client, occurredAt = ZonedDateTime.parse("2000-01-01T00:00:00Z"))
		val threshold = LocalDate.of(2000, 1, 1)

		// Act
		val result = port.record(old).then(port.record(older)).then(port.record(kept))
			.thenMany(Flux.concat(
				port.purgeOlderThan(threshold, dryRun = true),
				port.purgeOlderThan(threshold, dryRun = false),
				port.purgeOlderThan(threshold, dryRun = true),
			))
			.collectList()

		// Assert
		StepVerifier.create(result).expectNext(listOf(2L, 2L, 0L)).verifyComplete()
		StepVerifier.create(select(kept.actorUserId!!).map { it[2]!! }).expectNext("RATE_LIMITED").verifyComplete()
	}
}
