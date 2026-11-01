package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.test.TestContext
import java.time.ZoneOffset
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
class GroupMembershipFiltersRepositoryTest: TestContext() {
	@Autowired
	private lateinit var groupPort: IGroupPort

	@Autowired
	private lateinit var databaseClient: DatabaseClient

	private val projectId = UUID.randomUUID()
	private val otherProjectId = UUID.randomUUID()
	private val userId = UUID.randomUUID()
	private val strangerUserId = UUID.randomUUID()
	private val pageable = PageableModel(0, 200)
	private val arrival = ZonedDateTime.now(ZoneOffset.UTC).minusHours(4)

	@BeforeAll
	fun setup() {
		listOf(projectId, otherProjectId).forEach {
			execute("INSERT INTO tb_project (id, name, options) VALUES (:id, 'Membership', '{}'::TEXT[])", "id" to it)
		}
		listOf(userId, strangerUserId).forEach {
			execute("INSERT INTO tb_user (id, email) VALUES (:id, :email)", "id" to it, "email" to "$it@test.local")
		}
		insertGroup("a-mine-on-site", projectId, Member(userId, arrived = true))
		insertGroup("b-other-on-site", projectId, Member(null, arrived = true))
		insertGroup("c-mine-not-arrived", projectId, Member(userId, arrived = false))
		insertGroup("d-other-departed", projectId, Member(null, arrived = true, departed = true))
		insertGroup("e-hidden-member", projectId, Member(userId, arrived = true, visible = false), Member(null, arrived = false))
		insertGroup("f-other-project", otherProjectId, Member(userId, arrived = true))
	}

	@AfterAll
	fun cleanup() {
		listOf(projectId, otherProjectId).forEach { execute("DELETE FROM tb_project WHERE id = :id", "id" to it) }
		listOf(userId, strangerUserId).forEach { execute("DELETE FROM tb_user WHERE id = :id", "id" to it) }
	}

	@Test
	fun `Should keep only the groups having a visible member linked to the user when asked for mine`() {
		// Arrange
		val searchParams = GroupSearchParamModel(memberUserId = userId)

		// Act
		val names = findGroupNames(searchParams)

		// Assert
		assertEquals(listOf("a-mine-on-site", "c-mine-not-arrived"), names)
	}

	@Test
	fun `Should return no group when the user is linked to no participant`() {
		// Arrange
		val searchParams = GroupSearchParamModel(memberUserId = strangerUserId)

		// Act
		val names = findGroupNames(searchParams)

		// Assert
		assertEquals(emptyList(), names)
	}

	@Test
	fun `Should keep only the groups having a member that arrived and has not left when asked for on-site ones`() {
		// Arrange
		val searchParams = GroupSearchParamModel(hasOnSiteMembers = true)

		// Act
		val names = findGroupNames(searchParams)

		// Assert
		assertEquals(listOf("a-mine-on-site", "b-other-on-site"), names)
	}

	@Test
	fun `Should keep the groups without any on-site member when asked for the others`() {
		// Arrange
		val searchParams = GroupSearchParamModel(hasOnSiteMembers = false)

		// Act
		val names = findGroupNames(searchParams)

		// Assert
		assertEquals(listOf("c-mine-not-arrived", "d-other-departed", "e-hidden-member"), names)
	}

	@Test
	fun `Should combine my groups with the on-site ones`() {
		// Arrange
		val searchParams = GroupSearchParamModel(memberUserId = userId, hasOnSiteMembers = true)

		// Act
		val names = findGroupNames(searchParams)

		// Assert
		assertEquals(listOf("a-mine-on-site"), names)
	}

	@Test
	fun `Should count the filtered groups in the total of the page`() {
		// Arrange
		val searchParams = GroupSearchParamModel(memberUserId = userId)

		// Act
		val page = groupPort.findPage(projectId, PageableModel(0, 1), searchParams).block()!!

		// Assert
		assertEquals(2, page.totalElements)
		assertEquals(1, page.content.size)
	}

	private fun findGroupNames(searchParams: GroupSearchParamModel): List<String?> {
		return groupPort.findPage(projectId, pageable, searchParams).block()!!.content.map { it.name }
	}

	private data class Member(val userId: UUID?, val arrived: Boolean, val departed: Boolean = false, val visible: Boolean = true)

	private fun insertGroup(name: String, project: UUID, vararg members: Member) {
		val groupId = UUID.randomUUID()
		execute("INSERT INTO tb_group (id, name, project_id) VALUES (:id, :name, :p)", "id" to groupId, "name" to name, "p" to project)
		members.forEach { insertMember(name, project, groupId, it) }
	}

	private fun insertMember(name: String, project: UUID, groupId: UUID, member: Member) {
		val participantId = UUID.randomUUID()
		execute(
			"INSERT INTO tb_participant (id, first_name, last_name, birthday, type, project_id, user_id, visible, effective_arrival, effective_departure) " +
				"VALUES (:id, 'm', :name, CURRENT_DATE, 'REGISTERED', :p, :u, :v, :arrival, :departure)",
			"id" to participantId, "name" to name, "p" to project, "u" to member.userId, "v" to member.visible,
			"arrival" to (if (member.arrived) arrival else null), "departure" to (if (member.departed) arrival.plusHours(1) else null),
		)
		execute("INSERT INTO tb_group_content (group_id, participant_id) VALUES (:g, :s)", "g" to groupId, "s" to participantId)
	}

	private fun execute(sql: String, vararg binds: Pair<String, Any?>) {
		binds.fold(databaseClient.sql(sql)) { spec, (name, value) ->
			if (value == null) spec.bindNull(name, Any::class.java) else spec.bind(name, value)
		}.then().block()
	}
}
