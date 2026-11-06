package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.port.IProjectPort
import fr.laucoin.registry.backend.test.ModelExt.defaultUserId
import fr.laucoin.registry.backend.test.TestContext
import java.util.UUID
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient

class ProjectHiddenVisibilityIntegrationTest : TestContext() {
	@Autowired
	private lateinit var databaseClient: DatabaseClient

	@Autowired
	private lateinit var port: IProjectPort

	private companion object {
		const val SEARCH_QUERY = "Hidden visibility"
		val PAGE = PageableModel(0, 50)
	}

	private val visibleId = UUID.randomUUID()
	private val hiddenId = UUID.randomUUID()
	private val otherHiddenId = UUID.randomUUID()

	@BeforeEach
	fun insertProjects() {
		insertProject(visibleId, visible = true)
		insertProject(hiddenId, visible = false)
		insertProject(otherHiddenId, visible = false)
	}

	@AfterEach
	fun deleteProjects() {
		listOf(visibleId, hiddenId, otherHiddenId).forEach {
			databaseClient.sql("DELETE FROM tb_project WHERE id = :id").bind("id", it).then().block()
		}
	}

	@Test
	fun `Should findPage hide every hidden project from a viewer who can restore none`() {
		// Arrange
		val params = ProjectSearchParamModel(query = SEARCH_QUERY).forViewer(defaultUserId, emptyList())

		// Act
		val result = port.findPage(PAGE, params).block()!!

		// Assert
		assertEquals(setOf(visibleId), result.content.map { it.id }.toSet())
	}

	@Test
	fun `Should findPage reveal only the hidden projects the viewer can restore`() {
		// Arrange
		val params = ProjectSearchParamModel(query = SEARCH_QUERY).forViewer(defaultUserId, listOf(hiddenId))

		// Act
		val result = port.findPage(PAGE, params).block()!!

		// Assert
		assertEquals(setOf(visibleId, hiddenId), result.content.map { it.id }.toSet())
	}

	@Test
	fun `Should findPage keep a restorable hidden project out of the list when only visible ones are requested`() {
		// Arrange
		val params = ProjectSearchParamModel(query = SEARCH_QUERY, isVisible = true).forViewer(defaultUserId, listOf(hiddenId))

		// Act
		val result = port.findPage(PAGE, params).block()!!

		// Assert
		assertEquals(setOf(visibleId), result.content.map { it.id }.toSet())
	}

	@Test
	fun `Should findPage within the user projects apply the same hidden project rule`() {
		// Arrange
		val params = ProjectSearchParamModel(query = SEARCH_QUERY).forViewer(defaultUserId, listOf(hiddenId))

		// Act
		val result = port.findPage(listOf(visibleId, hiddenId, otherHiddenId), PAGE, params).block()!!

		// Assert
		assertEquals(setOf(visibleId, hiddenId), result.content.map { it.id }.toSet())
	}

	private fun insertProject(id: UUID, visible: Boolean) {
		databaseClient.sql("INSERT INTO tb_project (id, name, visible) VALUES (:id, :name, :visible)")
			.bind("id", id).bind("name", "$SEARCH_QUERY $id").bind("visible", visible).then().block()
	}
}
