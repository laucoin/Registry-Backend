package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectCountsModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.port.IProjectPort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IProjectEntityRepository
import fr.laucoin.registry.backend.test.ModelExt.defaultUserId
import fr.laucoin.registry.backend.test.TestContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ProjectCountsRepositoryTest : TestContext() {
	@Autowired
	private lateinit var databaseClient: DatabaseClient

	@MockitoSpyBean
	private lateinit var postgresRepository: IProjectEntityRepository

	@Autowired
	private lateinit var port: IProjectPort

	private val allOptionsProjectId = UUID.randomUUID()
	private val noOptionProjectId = UUID.randomUUID()
	private val emptyProjectId = UUID.randomUUID()
	private val allOptionsCounts = ProjectCountsModel(
		participants = 2,
		groups = 1,
		acceptedProfiles = 2,
		vehicles = 3,
		activities = 1,
		activeAlerts = 2,
	)

	@BeforeEach
	fun setup() {
		createProject(allOptionsProjectId, "'{VEHICLE,ACTIVITY,COMMUNICATION,ALERT}'")
		createProject(noOptionProjectId, "'{}'")
		createProject(emptyProjectId, "'{ALERT}'")
		databaseClient.sql("DELETE FROM tb_alert WHERE project_id = :id").bind("id", emptyProjectId).then().block()
		databaseClient.sql("DELETE FROM tb_participant WHERE project_id = :id AND visible IS TRUE")
			.bind("id", emptyProjectId).then().block()
	}

	@AfterEach
	fun cleanup() {
		listOf(allOptionsProjectId, noOptionProjectId, emptyProjectId).forEach {
			databaseClient.sql("DELETE FROM tb_project WHERE id = :id").bind("id", it).then().block()
		}
	}

	@Test
	fun `Should findById count only the visible elements and the matching statuses`() {
		// Arrange
		val id = allOptionsProjectId

		// Act
		val result = port.findById(id, isVisible = null).block()

		// Assert
		assertEquals(allOptionsCounts, result?.counts)
	}

	@Test
	fun `Should findById leave the counts of the disabled options empty`() {
		// Arrange
		val id = noOptionProjectId

		// Act
		val result = port.findById(id, isVisible = null).block()

		// Assert
		assertEquals(ProjectCountsModel(participants = 2, groups = 1, acceptedProfiles = 2), result?.counts)
	}

	@Test
	fun `Should findPage by project ids fill the exact counts`() {
		// Arrange
		val ids = listOf(allOptionsProjectId, noOptionProjectId)

		// Act
		val result = port.findPage(
			ids,
			PageableModel(0, 10),
			ProjectSearchParamModel(),
		).block()

		// Assert
		assertNotNull(result)
		assertEquals(2, result.content.size)
		assertEquals(allOptionsCounts, result.content.single { it.id == allOptionsProjectId }.counts)
		assertNull(result.content.single { it.id == noOptionProjectId }.counts?.vehicles)
	}

	@Test
	fun `Should update follow the enabled options in the returned counts`() {
		// Arrange
		val project = port.findById(allOptionsProjectId, isVisible = null).block()!!
		project.options = listOf(ProjectOptionEnum.VEHICLE)

		// Act
		val result = port.update(project).block()

		// Assert
		assertEquals(ProjectCountsModel(2, 1, 2, vehicles = 3), result?.counts)
	}

	@Test
	fun `Should update fail when the project cannot be reloaded after the save`() {
		// Arrange
		val project = port.findById(allOptionsProjectId, isVisible = null).block()!!
		doReturn(Mono.empty<ProjectEntity>()).whenever(postgresRepository)
			.findById(eq(allOptionsProjectId), anyOrNull())

		// Act
		val result = port.update(project)

		// Assert
		StepVerifier.create(result).expectError(IllegalStateException::class.java).verify()
	}

	@Test
	fun `Should findPage sort by activeAlerts descending with disabled option last`() {
		// Arrange
		val sort = SortModel("activeAlerts", isDescending = true)

		// Act
		val result = findSortedIds(sort)

		// Assert
		assertEquals(listOf(allOptionsProjectId, emptyProjectId, noOptionProjectId), result)
	}

	@Test
	fun `Should findPage sort by activeAlerts ascending with disabled option last`() {
		// Arrange
		val sort = SortModel("activeAlerts", isDescending = false)

		// Act
		val result = findSortedIds(sort)

		// Assert
		assertEquals(listOf(emptyProjectId, allOptionsProjectId, noOptionProjectId), result)
	}

	@Test
	fun `Should findPage sort by participants with the emptiest project at the edge`() {
		// Arrange
		val ascending = SortModel("participants", isDescending = false)
		val descending = SortModel("participants", isDescending = true)

		// Act
		val first = findSortedIds(ascending).first()
		val last = findSortedIds(descending).last()

		// Assert
		assertEquals(emptyProjectId, first)
		assertEquals(emptyProjectId, last)
	}

	private fun findSortedIds(sort: SortModel): List<UUID> {
		val ids = listOf(allOptionsProjectId, noOptionProjectId, emptyProjectId)
		val params = ProjectSearchParamModel(sort = sort)
		return port.findPage(ids, PageableModel(0, 10), params).block()!!.content.map { it.id!! }
	}

	private fun createProject(id: UUID, options: String) {
		databaseClient.sql("INSERT INTO tb_project (id, name, options) VALUES (:id, 'Counts', $options::TEXT[])")
			.bind("id", id).then().block()
		insertChildren(id)
	}

	private fun insertChildren(projectId: UUID) {
		insert(
			"INSERT INTO tb_participant (first_name, last_name, birthday, type, project_id, visible) " +
					"SELECT 'F', 'L', CURRENT_DATE, 'REGISTERED', :p, v FROM unnest(ARRAY[TRUE, TRUE, FALSE]) v",
			projectId
		)
		insert(
			"INSERT INTO tb_group (name, project_id, visible) SELECT 'G', :p, v FROM unnest(ARRAY[TRUE, FALSE]) v",
			projectId
		)
		insert(
			"INSERT INTO tb_vehicle (license_plate, brand, model, project_id, visible) " +
					"SELECT LEFT(CAST(gen_random_uuid() AS TEXT), 20), 'B', 'M', :p, v FROM unnest(ARRAY[TRUE, TRUE, TRUE, FALSE]) v",
			projectId
		)
		insert(
			"INSERT INTO tb_activity (name, project_id, visible) SELECT 'A', :p, v FROM unnest(ARRAY[TRUE, FALSE]) v",
			projectId
		)
		insertAlertsAndProfiles(projectId)
	}

	private fun insertAlertsAndProfiles(projectId: UUID) {
		insert(
			"INSERT INTO tb_alert (title, status, project_id, visible) SELECT 'T', s, :p, v FROM " +
					"(VALUES ('IN_PROGRESS', TRUE), ('IN_PROGRESS', TRUE), ('RESOLVED', TRUE), ('IN_PROGRESS', FALSE)) AS x(s, v)",
			projectId
		)
		databaseClient.sql(
			"INSERT INTO tb_project_profile (user_id, project_id, role, status, visible) SELECT :u, :p, 'PROJECT_ADMINISTRATOR', s, v FROM " +
					"(VALUES ('ACCEPTED', TRUE), ('ACCEPTED', TRUE), ('ACCEPTED', FALSE), ('INVITED', TRUE)) AS x(s, v)"
		).bind("u", defaultUserId).bind("p", projectId).then().block()
	}

	private fun insert(sql: String, projectId: UUID) {
		databaseClient.sql(sql).bind("p", projectId).then().block()
	}
}
