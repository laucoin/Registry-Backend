package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.test.TestContext
import java.time.LocalDate
import java.util.UUID
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient

class LastAdministratorQueryIntegrationTest : TestContext() {
	@Autowired
	private lateinit var databaseClient: DatabaseClient

	@Autowired
	private lateinit var port: IProjectProfilePort

	private companion object {
		val FAR_FUTURE: LocalDate = LocalDate.of(2999, 1, 1)
	}

	private val insertedProjects = mutableListOf<UUID>()
	private val insertedUsers = mutableListOf<UUID>()

	@AfterEach
	fun deleteInsertedRows() {
		insertedProjects.forEach { execute("DELETE FROM tb_project WHERE id = :id", it) }
		insertedUsers.forEach { execute("DELETE FROM tb_user WHERE id = :id", it) }
	}

	@Test
	fun `Should count only the permanent administrators of the project`() {
		// Arrange
		val projectId = insertProject()
		val permanent = insertUser()
		val temporary = insertUser()
		insertAdministratorProfile(projectId, permanent, endAccess = null)
		insertAdministratorProfile(projectId, temporary, endAccess = FAR_FUTURE)

		// Act
		val counts = port.findLevel0ProjectProfileRoleByUserId(permanent, isVisible = true).collectList().block()!!

		// Assert
		assertEquals(1, counts.single { it.project!!.id == projectId }.level0)
	}

	@Test
	fun `Should never treat a temporary administrator as the last administrator of the project`() {
		// Arrange
		val projectId = insertProject()
		val permanent = insertUser()
		val temporary = insertUser()
		insertAdministratorProfile(projectId, permanent, endAccess = null)
		insertAdministratorProfile(projectId, temporary, endAccess = FAR_FUTURE)

		// Act
		val counts = port.findLevel0ProjectProfileRoleByUserId(temporary, isVisible = true).collectList().block()!!

		// Assert
		assertTrue(counts.none { it.project!!.id == projectId })
	}

	@Test
	fun `Should count two permanent administrators of the same project`() {
		// Arrange
		val projectId = insertProject()
		val first = insertUser()
		insertAdministratorProfile(projectId, first, endAccess = null)
		insertAdministratorProfile(projectId, insertUser(), endAccess = null)

		// Act
		val counts = port.findLevel0ProjectProfileRoleByUserId(first, isVisible = true).collectList().block()!!

		// Assert
		assertEquals(2, counts.single { it.project!!.id == projectId }.level0)
	}

	private fun insertProject(): UUID {
		val id = UUID.randomUUID().also { insertedProjects.add(it) }
		databaseClient.sql("INSERT INTO tb_project(id, name) VALUES (:id, 'Last administrator')").bind("id", id).fetch().rowsUpdated().block()
		return id
	}

	private fun insertUser(): UUID {
		val id = UUID.randomUUID().also { insertedUsers.add(it) }
		databaseClient.sql(
			"INSERT INTO tb_user(id, oidc_id, type, first_name, last_name, email, role) VALUES (:id, :oidc, 'USER', 'Last', 'Administrator', :email, 'USER')"
		).bind("id", id).bind("oidc", UUID.randomUUID()).bind("email", "$id@last-administrator.test").fetch().rowsUpdated().block()
		return id
	}

	private fun insertAdministratorProfile(projectId: UUID, userId: UUID, endAccess: LocalDate?) {
		val spec = databaseClient.sql(
			"""
			INSERT INTO tb_project_profile(user_id, project_id, role, status, end_access_date)
			VALUES (:user, :project, 'PROJECT_ADMINISTRATOR', 'ACCEPTED', :end)
			"""
		).bind("user", userId).bind("project", projectId)
		val bound = if (endAccess == null) spec.bindNull("end", LocalDate::class.java) else spec.bind("end", endAccess)
		bound.fetch().rowsUpdated().block()
	}

	private fun execute(sql: String, id: UUID) {
		databaseClient.sql(sql).bind("id", id).fetch().rowsUpdated().block()
	}
}
