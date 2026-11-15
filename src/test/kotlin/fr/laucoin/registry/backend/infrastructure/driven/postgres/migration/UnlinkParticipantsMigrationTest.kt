package fr.laucoin.registry.backend.infrastructure.driven.postgres.migration

import fr.laucoin.registry.backend.test.TestContext
import java.util.UUID
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.r2dbc.core.DatabaseClient

class UnlinkParticipantsMigrationTest : TestContext() {
	@Autowired
	private lateinit var databaseClient: DatabaseClient

	private companion object {
		const val MIGRATION = "db/migrations/V1_29_0__unlink_participants_from_non_members.sql"
	}

	private val projectId = UUID.randomUUID()
	private val users = mutableListOf<UUID>()

	@AfterEach
	fun deleteRows() {
		execute("DELETE FROM tb_project WHERE id = :id", projectId)
		users.forEach { execute("DELETE FROM tb_user WHERE id = :id", it) }
	}

	@Test
	fun `Should unlink only the participants whose user has no visible accepted or invited profile on the project`() {
		// Arrange
		databaseClient.sql("INSERT INTO tb_project (id, name) VALUES (:id, 'Unlink migration')").bind("id", projectId).then().block()
		val accepted = linkedParticipant(profileStatus = "ACCEPTED", visible = true)
		val invited = linkedParticipant(profileStatus = "INVITED", visible = true)
		val rejected = linkedParticipant(profileStatus = "REJECTED", visible = true)
		val blocked = linkedParticipant(profileStatus = "ACCEPTED", visible = false)
		val outsider = linkedParticipant(profileStatus = null, visible = true)

		// Act
		databaseClient.sql(ClassPathResource(MIGRATION).getContentAsString(Charsets.UTF_8)).then().block()

		// Assert
		assertEquals(true, isLinked(accepted))
		assertEquals(true, isLinked(invited))
		assertEquals(false, isLinked(rejected))
		assertEquals(false, isLinked(blocked))
		assertEquals(false, isLinked(outsider))
	}

	private fun linkedParticipant(profileStatus: String?, visible: Boolean): UUID {
		val userId = UUID.randomUUID().also { users.add(it) }
		databaseClient.sql(
			"INSERT INTO tb_user(id, oidc_id, type, first_name, last_name, email, role) VALUES (:id, :oidc, 'USER', 'Unlink', 'User', :email, 'USER')"
		).bind("id", userId).bind("oidc", UUID.randomUUID()).bind("email", "$userId@unlink.test").then().block()
		if (profileStatus != null) {
			databaseClient.sql(
				"INSERT INTO tb_project_profile(user_id, project_id, role, status, visible) VALUES (:user, :project, 'PROJECT_ADMINISTRATOR', :status, :visible)"
			).bind("user", userId).bind("project", projectId).bind("status", profileStatus).bind("visible", visible).then().block()
		}
		val participantId = UUID.randomUUID()
		databaseClient.sql(
			"INSERT INTO tb_participant(id, first_name, last_name, birthday, type, project_id, user_id) VALUES (:id, 'Linked', 'Participant', '2015-01-01', 'REGISTERED', :project, :user)"
		).bind("id", participantId).bind("project", projectId).bind("user", userId).then().block()
		return participantId
	}

	private fun isLinked(participantId: UUID): Boolean = databaseClient.sql("SELECT user_id FROM tb_participant WHERE id = :id")
		.bind("id", participantId)
		.map { row -> row.get("user_id", UUID::class.java) != null }
		.one()
		.block()!!

	private fun execute(sql: String, id: UUID) {
		databaseClient.sql(sql).bind("id", id).then().block()
	}
}
