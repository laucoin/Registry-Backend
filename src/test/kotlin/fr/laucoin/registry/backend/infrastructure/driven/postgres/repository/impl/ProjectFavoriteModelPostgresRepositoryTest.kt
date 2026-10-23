package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.port.IProjectFavoritePort
import fr.laucoin.registry.backend.test.ModelExt.defaultUserId
import fr.laucoin.registry.backend.test.ModelExt.defaultUserIdWithoutProfile
import fr.laucoin.registry.backend.test.TestContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.r2dbc.core.DatabaseClient
import reactor.test.StepVerifier
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProjectFavoriteModelPostgresRepositoryTest : TestContext() {
	@Autowired
	private lateinit var databaseClient: DatabaseClient

	@Autowired
	private lateinit var port: IProjectFavoritePort

	private val firstProjectId = UUID.randomUUID()
	private val secondProjectId = UUID.randomUUID()
	private val temporaryUserId = UUID.randomUUID()

	@BeforeEach
	fun setup() {
		listOf(firstProjectId, secondProjectId).forEach {
			databaseClient.sql("INSERT INTO tb_project (id, name) VALUES (:id, 'Favorite')").bind("id", it).then()
				.block()
		}
		databaseClient.sql("INSERT INTO tb_user (id) VALUES (:id)").bind("id", temporaryUserId).then().block()
	}

	@AfterEach
	fun cleanup() {
		listOf(firstProjectId, secondProjectId).forEach {
			databaseClient.sql("DELETE FROM tb_project WHERE id = :id").bind("id", it).then().block()
		}
		databaseClient.sql("DELETE FROM tb_user WHERE id = :id").bind("id", temporaryUserId).then().block()
	}

	@Test
	fun `Should add keep a single favorite when called twice`() {
		// Arrange
		port.add(defaultUserId, firstProjectId).block()

		// Act
		port.add(defaultUserId, firstProjectId).block()

		// Assert
		assertEquals(1L, countFavorites(defaultUserId, firstProjectId))
	}

	@Test
	fun `Should remove delete the favorite`() {
		// Arrange
		port.add(defaultUserId, firstProjectId).block()

		// Act
		port.remove(defaultUserId, firstProjectId).block()

		// Assert
		assertEquals(0L, countFavorites(defaultUserId, firstProjectId))
	}

	@Test
	fun `Should remove ignore a missing favorite`() {
		// Arrange
		val missingProjectId = secondProjectId

		// Act
		val result = port.remove(defaultUserId, missingProjectId).block()

		// Assert
		assertEquals(Unit, result)
	}

	@Test
	fun `Should remove keep the favorite of another user`() {
		// Arrange
		port.add(defaultUserId, firstProjectId).block()
		port.add(defaultUserIdWithoutProfile, firstProjectId).block()

		// Act
		port.remove(defaultUserId, firstProjectId).block()

		// Assert
		assertEquals(1L, countFavorites(defaultUserIdWithoutProfile, firstProjectId))
	}

	@Test
	fun `Should the database refuse a duplicated favorite`() {
		// Arrange
		port.add(defaultUserId, firstProjectId).block()

		// Act
		val duplicate =
			databaseClient.sql("INSERT INTO tb_project_favorite (user_id, project_id) VALUES (:user, :project)")
				.bind("user", defaultUserId).bind("project", firstProjectId).then()

		// Assert
		StepVerifier.create(duplicate).expectError(DataIntegrityViolationException::class.java).verify()
	}

	@Test
	fun `Should add fail when the project does not exist`() {
		// Arrange
		val unknownProjectId = UUID.randomUUID()

		// Act
		val result = port.add(defaultUserId, unknownProjectId)

		// Assert
		StepVerifier.create(result).expectError(DataIntegrityViolationException::class.java).verify()
	}

	@Test
	fun `Should findFavoriteProjectIds return only the favorites of the given user`() {
		// Arrange
		port.add(defaultUserId, firstProjectId).block()
		port.add(defaultUserIdWithoutProfile, secondProjectId).block()

		// Act
		val result =
			port.findFavoriteProjectIds(defaultUserId, listOf(firstProjectId, secondProjectId)).collectList().block()

		// Assert
		assertEquals(listOf(firstProjectId), result)
	}

	@Test
	fun `Should findFavoriteProjectIds return every favorite among the given ids`() {
		// Arrange
		port.add(defaultUserId, firstProjectId).block()
		port.add(defaultUserId, secondProjectId).block()

		// Act
		val result =
			port.findFavoriteProjectIds(defaultUserId, listOf(firstProjectId, secondProjectId, UUID.randomUUID()))
				.collectList().block()

		// Assert
		assertEquals(setOf(firstProjectId, secondProjectId), result?.toSet())
	}

	@Test
	fun `Should findFavoriteProjectIds return nothing when no id is given`() {
		// Arrange
		port.add(defaultUserId, firstProjectId).block()

		// Act
		val result = port.findFavoriteProjectIds(defaultUserId, emptyList()).collectList().block()

		// Assert
		assertTrue(result!!.isEmpty())
	}

	@Test
	fun `Should delete the favorites when the project is deleted`() {
		// Arrange
		port.add(defaultUserId, firstProjectId).block()

		// Act
		databaseClient.sql("DELETE FROM tb_project WHERE id = :id").bind("id", firstProjectId).then().block()

		// Assert
		assertEquals(0L, countFavorites(defaultUserId, firstProjectId))
	}

	@Test
	fun `Should delete the favorites when the user is deleted`() {
		// Arrange
		port.add(temporaryUserId, firstProjectId).block()

		// Act
		databaseClient.sql("DELETE FROM tb_user WHERE id = :id").bind("id", temporaryUserId).then().block()

		// Assert
		assertEquals(0L, countFavorites(temporaryUserId, firstProjectId))
	}

	private fun countFavorites(user: UUID, project: UUID): Long {
		return databaseClient.sql("SELECT COUNT(*) FROM tb_project_favorite WHERE user_id = :user AND project_id = :project")
			.bind("user", user).bind("project", project)
			.map { row -> row.get(0, Long::class.javaObjectType)!! }.one().block()!!
	}
}
