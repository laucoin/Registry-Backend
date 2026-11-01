package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.test.TestContext
import java.time.ZoneOffset
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient

@TestInstance(PER_CLASS)
class LastMovementProjectScopeRepositoryTest: TestContext() {
	@Autowired
	private lateinit var participantPort: IParticipantPort

	@Autowired
	private lateinit var groupPort: IGroupPort

	@Autowired
	private lateinit var databaseClient: DatabaseClient

	private val projectId = UUID.randomUUID()
	private val otherProjectId = UUID.randomUUID()
	private val groupId = UUID.randomUUID()
	private val siblingGroupId = UUID.randomUUID()
	private val memberId = UUID.randomUUID()
	private val siblingId = UUID.randomUUID()
	private val strangerId = UUID.randomUUID()
	private val futureStrangerId = UUID.randomUUID()
	private val sharedInstant = ZonedDateTime.now(ZoneOffset.UTC).minusHours(3).truncatedTo(ChronoUnit.SECONDS)

	@BeforeAll
	fun setup() {
		listOf(projectId, otherProjectId).forEach {
			execute("INSERT INTO tb_project (id, name, options) VALUES (:id, 'Scope', '{}'::TEXT[])", "id" to it)
		}
		insertGroup(groupId, projectId)
		insertGroup(siblingGroupId, projectId)
		insertMember(memberId, projectId, "IN", sharedInstant, groupId)
		insertMember(siblingId, projectId, "OUT", sharedInstant, siblingGroupId)
		insertMember(strangerId, otherProjectId, "IN", sharedInstant, null)
		insertMember(futureStrangerId, otherProjectId, "IN", sharedInstant.plusDays(2), null)
	}

	@AfterAll
	fun cleanup() {
		listOf(projectId, otherProjectId).forEach { execute("DELETE FROM tb_project WHERE id = :id", "id" to it) }
	}

	@Test
	fun `Should keep the status of a participant whatever the movements made at the same instant elsewhere`() {
		// Arrange
		val pageable = PageableModel(0, 200)
		val searchParams = ParticipantSearchParamModel(query = null, at = null)

		// Act
		val page = participantPort.findPage(projectId, pageable, searchParams).block()!!

		// Assert
		assertEquals(setOf(memberId, siblingId), page.content.mapNotNull { it.id }.toSet())
		assertEquals(listOf(PresenceStatusEnum.IN), page.content.filter { it.id == memberId }.map { it.status })
		assertEquals(listOf(PresenceStatusEnum.OUT), page.content.filter { it.id == siblingId }.map { it.status })
	}

	@Test
	fun `Should count the inside members of a group once whatever the movements made at the same instant elsewhere`() {
		// Arrange
		val ids = listOf(groupId, siblingGroupId)

		// Act
		val groups = ids.map { groupPort.findByIdWithContent(projectId, it, null, null, null).block()!! }

		// Assert
		assertEquals(listOf(1L, 0L), groups.map { it.insideMembersCount })
		assertEquals(listOf(1L, 1L), groups.map { it.membersCount })
	}

	@Test
	fun `Should select the participants of every project whose last movement is old in the cross project purge`() {
		// Arrange
		val threshold = LocalDate.now(ZoneOffset.UTC).plusDays(1)

		// Act
		val unused = participantPort.findUnusedSince(threshold).collectList().block()!!

		// Assert
		assertTrue(unused.containsAll(listOf(memberId, siblingId, strangerId)))
		assertFalse(unused.contains(futureStrangerId))
	}

	private fun insertGroup(id: UUID, project: UUID) {
		execute("INSERT INTO tb_group (id, name, project_id) VALUES (:id, 'scope group', :p)", "id" to id, "p" to project)
	}

	private fun insertMember(id: UUID, project: UUID, movementType: String, at: ZonedDateTime, group: UUID?) {
		val movementId = UUID.randomUUID()
		execute(
			"INSERT INTO tb_participant (id, first_name, last_name, birthday, type, project_id, effective_arrival) " +
				"VALUES (:id, 'scope', 'member', CURRENT_DATE, 'REGISTERED', :p, :at)",
			"id" to id, "p" to project, "at" to sharedInstant,
		)
		execute(
			"INSERT INTO tb_movement (id, date_time, type, project_id) VALUES (:id, :at, :type, :p)",
			"id" to movementId, "at" to at, "type" to movementType, "p" to project,
		)
		execute("INSERT INTO tb_movement_content (movement_id, participant_id) VALUES (:m, :s)", "m" to movementId, "s" to id)
		group?.let { execute("INSERT INTO tb_group_content (group_id, participant_id) VALUES (:g, :s)", "g" to it, "s" to id) }
	}

	private fun execute(sql: String, vararg binds: Pair<String, Any>) {
		binds.fold(databaseClient.sql(sql)) { spec, (name, value) -> spec.bind(name, value) }.then().block()
	}
}
