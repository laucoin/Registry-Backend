package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.port.IProjectFavoritePort
import fr.laucoin.registry.backend.domain.port.IProjectPort
import fr.laucoin.registry.backend.test.ModelExt.defaultUserId
import fr.laucoin.registry.backend.test.ModelExt.defaultUserIdWithoutProfile
import fr.laucoin.registry.backend.test.TestContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient
import java.util.UUID
import kotlin.test.assertEquals

class ProjectFavoriteSearchRepositoryTest : TestContext() {
	@Autowired
	private lateinit var databaseClient: DatabaseClient

	@Autowired
	private lateinit var projectPort: IProjectPort

	@Autowired
	private lateinit var favoritePort: IProjectFavoritePort

	private companion object {
		const val SEARCH_QUERY = "Favorite search"
	}

	private val firstProjectId = UUID.randomUUID()
	private val secondProjectId = UUID.randomUUID()
	private val thirdProjectId = UUID.randomUUID()
	private val allProjectIds = listOf(firstProjectId, secondProjectId, thirdProjectId)

	@BeforeEach
	fun setup() {
		allProjectIds.forEachIndexed { index, id ->
			databaseClient.sql("INSERT INTO tb_project (id, name) VALUES (:id, :name)")
				.bind("id", id).bind("name", "Favorite search $index").then().block()
		}
		favoritePort.add(defaultUserId, firstProjectId).block()
		favoritePort.add(defaultUserId, thirdProjectId).block()
		favoritePort.add(defaultUserIdWithoutProfile, secondProjectId).block()
	}

	@AfterEach
	fun cleanup() {
		allProjectIds.forEach {
			databaseClient.sql("DELETE FROM tb_project WHERE id = :id").bind("id", it).then().block()
		}
	}

	@Test
	fun `Should findPage keep only the favorites of the user`() {
		// Arrange
		val params = searchParams(isFavorite = true)

		// Act
		val result = findIds(params)

		// Assert
		assertEquals(setOf(firstProjectId, thirdProjectId), result.toSet())
	}

	@Test
	fun `Should findPage keep only the projects that are not favorites of the user`() {
		// Arrange
		val params = searchParams(isFavorite = false)

		// Act
		val result = findIds(params)

		// Assert
		assertEquals(listOf(secondProjectId), result)
	}

	@Test
	fun `Should findPage ignore the favorite filter when it is omitted`() {
		// Arrange
		val params = searchParams(isFavorite = null)

		// Act
		val result = findIds(params)

		// Assert
		assertEquals(allProjectIds.toSet(), result.toSet())
	}

	@Test
	fun `Should findPage not leak the favorites of another user`() {
		// Arrange
		val params = searchParams(isFavorite = true, user = defaultUserIdWithoutProfile)

		// Act
		val result = findIds(params)

		// Assert
		assertEquals(listOf(secondProjectId), result)
	}

	@Test
	fun `Should findPage sort the favorites first when sorting by favorite descending`() {
		// Arrange
		val params = searchParams(sort = SortModel("favorite", isDescending = true))

		// Act
		val result = findIds(params)

		// Assert
		assertEquals(listOf(firstProjectId, thirdProjectId, secondProjectId), result)
	}

	@Test
	fun `Should findPage sort the favorites last when sorting by favorite ascending`() {
		// Arrange
		val params = searchParams(sort = SortModel("favorite", isDescending = false))

		// Act
		val result = findIds(params)

		// Assert
		assertEquals(listOf(secondProjectId, firstProjectId, thirdProjectId), result)
	}

	@Test
	fun `Should findPage without ids keep only the favorites of the user`() {
		// Arrange
		val params = searchParams(isFavorite = true, query = SEARCH_QUERY)

		// Act
		val result = projectPort.findPage(PageableModel(0, 10), params).block()!!.content.map { it.id!! }

		// Assert
		assertEquals(listOf(firstProjectId, thirdProjectId), result)
	}

	@Test
	fun `Should findPage without ids sort the favorites first`() {
		// Arrange
		val params = searchParams(sort = SortModel("favorite", isDescending = true), query = SEARCH_QUERY)

		// Act
		val result = projectPort.findPage(PageableModel(0, 10), params).block()!!.content.map { it.id!! }

		// Assert
		assertEquals(listOf(firstProjectId, thirdProjectId, secondProjectId), result)
	}

	@Test
	fun `Should findPage count every favorite when the page is smaller`() {
		// Arrange
		val params = searchParams(isFavorite = true)

		// Act
		val result = projectPort.findPage(allProjectIds, PageableModel(0, 1), params).block()!!

		// Assert
		assertEquals(2, result.totalElements)
		assertEquals(listOf(firstProjectId), result.content.map { it.id })
	}

	@Test
	fun `Should findPage return nothing when the user has no favorite`() {
		// Arrange
		val params = searchParams(isFavorite = true, user = UUID.randomUUID())

		// Act
		val result = projectPort.findPage(allProjectIds, PageableModel(0, 10), params).block()!!

		// Assert
		assertEquals(0, result.totalElements)
	}

	@Test
	fun `Should findPage keep only the favorites when the filter is combined with the active alerts sort`() {
		// Arrange
		val params = searchParams(isFavorite = true, sort = SortModel("activeAlerts", isDescending = true))

		// Act
		val result = findIds(params)

		// Assert
		assertEquals(setOf(firstProjectId, thirdProjectId), result.toSet())
	}

	private fun searchParams(
		isFavorite: Boolean? = null,
		sort: SortModel? = null,
		user: UUID = defaultUserId,
		query: String? = null,
	) = ProjectSearchParamModel(query = query, sort = sort, isFavorite = isFavorite).apply { userId = user }

	private fun findIds(params: ProjectSearchParamModel): List<UUID> {
		return projectPort.findPage(allProjectIds, PageableModel(0, 10), params).block()!!.content.map { it.id!! }
	}
}
