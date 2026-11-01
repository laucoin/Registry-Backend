package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IGroupContentEntityRepository
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
class GroupOnSiteCountsRepositoryTest: TestContext() {
	@Autowired
	private lateinit var groupPort: IGroupPort

	@Autowired
	private lateinit var contentRepository: IGroupContentEntityRepository

	@Autowired
	private lateinit var databaseClient: DatabaseClient

	private val projectId = UUID.randomUUID()
	private val otherProjectId = UUID.randomUUID()
	private val arrival = ZonedDateTime.now(ZoneOffset.UTC).minusHours(5)
	private val pageable = PageableModel(0, 200)
	private lateinit var otherProjectGroupId: UUID

	private data class Member(
		val movements: List<String>,
		val arrived: Boolean = true,
		val departed: Boolean = false,
		val visible: Boolean = true,
		val plannedEndPassed: Boolean = false,
	)

	@BeforeAll
	fun setup() {
		listOf(projectId, otherProjectId).forEach {
			execute("INSERT INTO tb_project (id, name, options) VALUES (:id, 'Counts', '{}'::TEXT[])", "id" to it)
		}
		insertGroup(
			"a-mixed",
			projectId,
			Member(listOf("IN")),
			Member(listOf("IN", "OUT")),
			Member(listOf("IN"), plannedEndPassed = true),
			Member(listOf("IN", "OUT"), departed = true),
			Member(emptyList(), arrived = false),
			Member(listOf("IN"), visible = false),
		)
		insertGroup("b-nobody-on-site", projectId, Member(listOf("IN", "OUT"), departed = true), Member(emptyList(), arrived = false))
		insertGroup("c-crowd", projectId, *Array(30) { Member(listOf("IN")) })
		otherProjectGroupId = insertGroup("d-other-project", otherProjectId, Member(listOf("IN")))
	}

	@AfterAll
	fun cleanup() {
		listOf(projectId, otherProjectId).forEach { execute("DELETE FROM tb_project WHERE id = :id", "id" to it) }
	}

	@Test
	fun `Should split the on-site members of a group by presence status`() {
		// Arrange
		val searchParams = GroupSearchParamModel(query = "a-mixed")

		// Act
		val group = findGroups(searchParams).single()

		// Assert
		assertEquals(listOf(3L, 1L, 1L, 1L), counts(group))
	}

	@Test
	fun `Should count no on-site member for a group whose members left or did not arrive`() {
		// Arrange
		val searchParams = GroupSearchParamModel(query = "b-nobody")

		// Act
		val group = findGroups(searchParams).single()

		// Assert
		assertEquals(listOf(0L, 0L, 0L, 0L), counts(group))
	}

	@Test
	fun `Should count the on-site members of a large group`() {
		// Arrange
		val searchParams = GroupSearchParamModel(query = "c-crowd")

		// Act
		val group = findGroups(searchParams).single()

		// Assert
		assertEquals(listOf(30L, 30L, 0L, 0L), counts(group))
		assertEquals(30L, group.membersCount)
	}

	@Test
	fun `Should give the counters of every group of the page with one answer per group`() {
		// Arrange
		val searchParams = GroupSearchParamModel(hasOnSiteMembers = true)

		// Act
		val groups = findGroups(searchParams)

		// Assert
		assertEquals(listOf("a-mixed", "c-crowd"), groups.map { it.name })
		assertEquals(listOf(listOf(3L, 1L, 1L, 1L), listOf(30L, 30L, 0L, 0L)), groups.map { counts(it) })
	}

	@Test
	fun `Should return an empty page without counters when no group matches`() {
		// Arrange
		val searchParams = GroupSearchParamModel(query = "does-not-exist")

		// Act
		val page = groupPort.findPage(projectId, pageable, searchParams).block()!!

		// Assert
		assertEquals(0, page.totalElements)
		assertEquals(emptyList(), page.content)
	}

	@Test
	fun `Should give the counters of the groups of the requested page only`() {
		// Arrange
		val searchParams = GroupSearchParamModel(hasOnSiteMembers = true)

		// Act
		val first = groupPort.findPage(projectId, PageableModel(0, 1), searchParams).block()!!
		val second = groupPort.findPage(projectId, PageableModel(1, 1), searchParams).block()!!

		// Assert
		assertEquals(listOf("a-mixed"), first.content.map { it.name })
		assertEquals(listOf(listOf(3L, 1L, 1L, 1L)), first.content.map { counts(it) })
		assertEquals(listOf("c-crowd"), second.content.map { it.name })
		assertEquals(listOf(listOf(30L, 30L, 0L, 0L)), second.content.map { counts(it) })
	}

	@Test
	fun `Should leave the counters empty when a single group is read`() {
		// Arrange
		val groupId = findGroups(GroupSearchParamModel(query = "a-mixed")).single().id!!

		// Act
		val group = groupPort.findByIdWithContent(projectId, groupId, null, null, null).block()!!

		// Assert
		assertEquals(listOf(null, null, null, null), counts(group))
	}

	@Test
	fun `Should never count the members of a group of another project`() {
		// Act
		val counts = contentRepository.findOnSiteCounts(projectId, listOf(otherProjectGroupId), null).collectList().block()!!

		// Assert
		assertEquals(emptyList(), counts)
	}

	private fun counts(group: GroupModel): List<Long?> {
		return listOf(group.onSiteMembersCount, group.inMembersCount, group.outMembersCount, group.overdueMembersCount)
	}

	private fun findGroups(searchParams: GroupSearchParamModel): List<GroupModel> {
		return groupPort.findPage(projectId, pageable, searchParams).block()!!.content
	}

	private fun insertGroup(name: String, project: UUID, vararg members: Member): UUID {
		val groupId = UUID.randomUUID()
		execute("INSERT INTO tb_group (id, name, project_id) VALUES (:id, :name, :p)", "id" to groupId, "name" to name, "p" to project)
		members.forEachIndexed { index, member -> insertMember("$name-$index", project, groupId, member) }
		return groupId
	}

	private fun insertMember(name: String, project: UUID, groupId: UUID, member: Member) {
		val participantId = UUID.randomUUID()
		execute(
			"INSERT INTO tb_participant (id, first_name, last_name, birthday, type, project_id, visible, effective_arrival, effective_departure, end_availability_date) " +
				"VALUES (:id, 'm', :name, CURRENT_DATE, 'REGISTERED', :p, :v, :arrival, :departure, :end)",
			"id" to participantId, "name" to name, "p" to project, "v" to member.visible,
			"arrival" to (if (member.arrived) arrival else null), "departure" to (if (member.departed) arrival.plusHours(3) else null),
			"end" to (if (member.plannedEndPassed) LocalDate.now(ZoneOffset.UTC).minusDays(1) else null),
		)
		execute("INSERT INTO tb_group_content (group_id, participant_id) VALUES (:g, :s)", "g" to groupId, "s" to participantId)
		member.movements.forEachIndexed { index, type -> insertMovement(participantId, project, type, arrival.plusHours(index.toLong())) }
	}

	private fun insertMovement(participantId: UUID, project: UUID, type: String, at: ZonedDateTime) {
		val movementId = UUID.randomUUID()
		execute(
			"INSERT INTO tb_movement (id, date_time, type, project_id) VALUES (:id, :at, :type, :p)",
			"id" to movementId, "at" to at, "type" to type, "p" to project,
		)
		execute("INSERT INTO tb_movement_content (movement_id, participant_id) VALUES (:m, :s)", "m" to movementId, "s" to participantId)
	}

	private fun execute(sql: String, vararg binds: Pair<String, Any?>) {
		binds.fold(databaseClient.sql(sql)) { spec, (name, value) ->
			if (value == null) spec.bindNull(name, Any::class.java) else spec.bind(name, value)
		}.then().block()
	}
}
