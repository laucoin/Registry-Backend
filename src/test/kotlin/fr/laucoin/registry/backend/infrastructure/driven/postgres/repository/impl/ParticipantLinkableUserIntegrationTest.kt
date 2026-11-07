package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.test.TestContext
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient

class ParticipantLinkableUserIntegrationTest : TestContext() {
	@Autowired
	private lateinit var databaseClient: DatabaseClient

	@Autowired
	private lateinit var port: IParticipantPort

	private val projectId = UUID.randomUUID()
	private val otherProjectId = UUID.randomUUID()
	private val userId = UUID.randomUUID()

	private companion object {
		@JvmStatic
		fun profiles(): Stream<Arguments> = Stream.of(
			Arguments.of("ACCEPTED", true, true, true),
			Arguments.of("INVITED", true, true, true),
			Arguments.of("REJECTED", true, true, false),
			Arguments.of("ACCEPTED", false, true, false),
			Arguments.of("ACCEPTED", true, false, false),
		)
	}

	@BeforeEach
	fun insertProjectsAndUser() {
		listOf(projectId, otherProjectId).forEach {
			databaseClient.sql("INSERT INTO tb_project (id, name) VALUES (:id, 'Linkable user')").bind("id", it).then().block()
		}
		databaseClient.sql(
			"INSERT INTO tb_user(id, oidc_id, type, first_name, last_name, email, role) VALUES (:id, :oidc, 'USER', 'Linkable', 'User', :email, 'USER')"
		).bind("id", userId).bind("oidc", UUID.randomUUID()).bind("email", "$userId@linkable.test").then().block()
	}

	@AfterEach
	fun deleteRows() {
		listOf(projectId, otherProjectId).forEach {
			databaseClient.sql("DELETE FROM tb_project WHERE id = :id").bind("id", it).then().block()
		}
		databaseClient.sql("DELETE FROM tb_user WHERE id = :id").bind("id", userId).then().block()
	}

	@ParameterizedTest
	@MethodSource("profiles")
	fun `Should isProjectMember accept only a visible accepted or invited profile on the same project`(
		status: String,
		visible: Boolean,
		sameProject: Boolean,
		expected: Boolean,
	) {
		// Arrange
		databaseClient.sql(
			"INSERT INTO tb_project_profile(user_id, project_id, role, status, visible) VALUES (:user, :project, 'PROJECT_ADMINISTRATOR', :status, :visible)"
		).bind("user", userId).bind("project", if (sameProject) projectId else otherProjectId)
			.bind("status", status).bind("visible", visible).then().block()

		// Act
		val result = port.isProjectMember(projectId, userId).block()

		// Assert
		assertEquals(expected, result)
	}
}
