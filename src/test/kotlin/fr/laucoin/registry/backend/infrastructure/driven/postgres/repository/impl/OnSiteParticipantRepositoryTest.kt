package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.test.TestContext
import java.time.ZoneOffset
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient

@TestInstance(PER_CLASS)
class OnSiteParticipantRepositoryTest: TestContext() {
	@Autowired
	private lateinit var participantPort: IParticipantPort

	@Autowired
	private lateinit var databaseClient: DatabaseClient

	private val projectId = UUID.randomUUID()
	private val groupId = UUID.randomUUID()
	private val pageable = PageableModel(0, 200)
	private val arrival = ZonedDateTime.now(ZoneOffset.UTC).minusHours(5)

	@BeforeAll
	fun setup() {
		execute("INSERT INTO tb_project (id, name, options) VALUES (:id, 'On site', '{}'::TEXT[])", "id" to projectId)
		execute("INSERT INTO tb_group (id, name, project_id) VALUES (:id, 'on site group', :p)", "id" to groupId, "p" to projectId)
		insertParticipant("a-inside", arrived = true, movements = listOf("IN"))
		insertParticipant("b-outside", arrived = true, movements = listOf("IN", "OUT"))
		insertParticipant("c-overdue", arrived = true, movements = listOf("IN"), plannedEndPassed = true)
		insertParticipant("d-departed", arrived = true, movements = listOf("IN", "OUT"), departed = true)
		insertParticipant("e-expected", arrived = false, movements = emptyList())
	}

	@AfterAll
	fun cleanup() {
		execute("DELETE FROM tb_project WHERE id = :id", "id" to projectId)
	}

	@Test
	fun `Should keep only the participants that arrived and have not left when asked for on-site ones`() {
		// Arrange
		val searchParams = ParticipantSearchParamModel(query = null, at = null, isOnSite = true)

		// Act
		val names = findGroupMemberNames(searchParams)

		// Assert
		assertEquals(setOf("a-inside", "b-outside", "c-overdue"), names.toSet())
	}

	@Test
	fun `Should keep the participants that left or did not arrive when asked for the others`() {
		// Arrange
		val searchParams = ParticipantSearchParamModel(query = null, at = null, isOnSite = false)

		// Act
		val names = findGroupMemberNames(searchParams)

		// Assert
		assertEquals(setOf("d-departed", "e-expected"), names.toSet())
	}

	@Test
	fun `Should keep every participant when the on-site filter is omitted`() {
		// Arrange
		val searchParams = ParticipantSearchParamModel(query = null, at = null)

		// Act
		val names = findGroupMemberNames(searchParams)

		// Assert
		assertEquals(5, names.size)
	}

	@Test
	fun `Should sort the on-site participants by urgency of their status`() {
		// Arrange
		val searchParams = ParticipantSearchParamModel(query = null, at = null, isOnSite = true, sort = SortModel("status"))

		// Act
		val members = findGroupMembers(searchParams)

		// Assert
		assertEquals(listOf("c-overdue", "b-outside", "a-inside"), members.map { it.lastName })
		assertEquals(listOf(PresenceStatusEnum.OVERDUE, PresenceStatusEnum.OUT, PresenceStatusEnum.IN), members.map { it.status })
	}

	@Test
	fun `Should reverse the status order when sorting in descending order`() {
		// Arrange
		val searchParams = ParticipantSearchParamModel(query = null, at = null, isOnSite = true, sort = SortModel("status", true))

		// Act
		val names = findGroupMemberNames(searchParams)

		// Assert
		assertEquals(listOf("a-inside", "b-outside", "c-overdue"), names)
	}

	@Test
	fun `Should filter the on-site participants of the whole project`() {
		// Arrange
		val searchParams = ParticipantSearchParamModel(query = null, at = null, isOnSite = true)

		// Act
		val page = participantPort.findPage(projectId, pageable, searchParams).block()!!

		// Assert
		assertEquals(3, page.totalElements)
		assertEquals(setOf("a-inside", "b-outside", "c-overdue"), page.content.mapNotNull { it.lastName }.toSet())
	}

	@Test
	fun `Should rank the departed participants last and the expected ones with the outside ones when the on-site filter is omitted`() {
		// Arrange
		val searchParams = ParticipantSearchParamModel(query = null, at = null, sort = SortModel("status"))

		// Act
		val names = findGroupMemberNames(searchParams)

		// Assert
		assertEquals(listOf("c-overdue", "b-outside", "e-expected", "a-inside", "d-departed"), names)
	}

	@Test
	fun `Should combine the on-site filter with the status filter`() {
		// Arrange
		val searchParams = ParticipantSearchParamModel(query = null, status = PresenceStatusEnum.OUT, at = null, isOnSite = true)

		// Act
		val names = findGroupMemberNames(searchParams)

		// Assert
		assertEquals(listOf("b-outside"), names)
	}

	@Test
	fun `Should page the on-site participants without losing nor repeating any`() {
		// Arrange
		val searchParams = ParticipantSearchParamModel(query = null, at = null, isOnSite = true, sort = SortModel("status"))

		// Act
		val first = participantPort.findPageByGroupId(projectId, groupId, PageableModel(0, 2), searchParams).block()!!
		val second = participantPort.findPageByGroupId(projectId, groupId, PageableModel(2, 2), searchParams).block()!!

		// Assert
		assertEquals(3, first.totalElements)
		assertEquals(3, second.totalElements)
		assertEquals(listOf("c-overdue", "b-outside"), first.content.map { it.lastName })
		assertEquals(listOf("a-inside"), second.content.map { it.lastName })
	}

	private fun findGroupMembers(searchParams: ParticipantSearchParamModel): List<ParticipantModel> {
		return participantPort.findPageByGroupId(projectId, groupId, pageable, searchParams).block()!!.content
	}

	private fun findGroupMemberNames(searchParams: ParticipantSearchParamModel): List<String?> {
		return findGroupMembers(searchParams).map { it.lastName }
	}

	private fun insertParticipant(
		lastName: String,
		arrived: Boolean,
		movements: List<String>,
		departed: Boolean = false,
		plannedEndPassed: Boolean = false,
	) {
		val id = UUID.randomUUID()
		execute(
			"INSERT INTO tb_participant (id, first_name, last_name, birthday, type, project_id, effective_arrival, effective_departure, end_availability_date) " +
				"VALUES (:id, 'on', :name, CURRENT_DATE, 'REGISTERED', :p, :arrival, :departure, :end)",
			"id" to id, "name" to lastName, "p" to projectId,
			"arrival" to (if (arrived) arrival else null), "departure" to (if (departed) arrival.plusHours(2) else null),
			"end" to (if (plannedEndPassed) LocalDate.now(ZoneOffset.UTC).minusDays(1) else null),
		)
		execute("INSERT INTO tb_group_content (group_id, participant_id) VALUES (:g, :s)", "g" to groupId, "s" to id)
		movements.forEachIndexed { index, type -> insertMovement(id, type, arrival.plusHours(index.toLong())) }
	}

	private fun insertMovement(participantId: UUID, type: String, at: ZonedDateTime) {
		val movementId = UUID.randomUUID()
		execute(
			"INSERT INTO tb_movement (id, date_time, type, project_id) VALUES (:id, :at, :type, :p)",
			"id" to movementId, "at" to at, "type" to type, "p" to projectId,
		)
		execute("INSERT INTO tb_movement_content (movement_id, participant_id) VALUES (:m, :s)", "m" to movementId, "s" to participantId)
	}

	private fun execute(sql: String, vararg binds: Pair<String, Any?>) {
		binds.fold(databaseClient.sql(sql)) { spec, (name, value) ->
			if (value == null) spec.bindNull(name, Any::class.java) else spec.bind(name, value)
		}.then().block()
	}
}
