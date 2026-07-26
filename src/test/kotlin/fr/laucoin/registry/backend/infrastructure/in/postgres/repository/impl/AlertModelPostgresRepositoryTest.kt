package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.AlertSortFieldEnum
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.port.IAlertPort
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AlertModelPostgresRepositoryTest : TestContext() {
	@Autowired
	private lateinit var repository: IAlertPort

	@Test
	fun `Should findPage call repository count and findPage`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = AlertSearchParamModel()

		// Act
		val result = repository.findPage(projectId, pageable, params).block()

		// Assert
		assertNotNull(result)
		assertEquals(0, result.pageNumber)
		assertEquals(10, result.pageSize)
		assertEquals(50, result.totalElements)
		assertEquals(5, result.totalPages)
	}

	@Test
	fun `Should findPage execute the sorted query and order by status then title descending`() {
		// Arrange
		val pageable = PageableModel(0, 50)
		val params = AlertSearchParamModel()
		val sort = listOf(
			SortModel(AlertSortFieldEnum.STATUS),
			SortModel(AlertSortFieldEnum.TITLE, descending = true),
		)

		// Act
		val result = repository.findPage(projectId, pageable, params, sort).block()

		// Assert
		assertNotNull(result)
		assertEquals(50, result.totalElements)
		assertEquals(50, result.content.size)
		val expectedOrder = result.content
			.sortedWith(compareBy<AlertModel> { it.status!!.name }.thenByDescending { it.title })
			.map { it.id }
		assertEquals(expectedOrder, result.content.map { it.id })
	}

	@Test
	fun `Should findPage combine the visibility filter with the sorted query`() {
		// Arrange
		val pageable = PageableModel(0, 50)
		val params = AlertSearchParamModel(textSearched = null, visibilitySearched = true)
		val sort = listOf(SortModel(AlertSortFieldEnum.TITLE, descending = true))

		// Act
		val result = repository.findPage(projectId, pageable, params, sort).block()

		// Assert
		assertNotNull(result)
		assertEquals(49, result.totalElements)
		assertEquals(49, result.content.size)
		assertTrue(result.content.all { it.visible })
		val expectedOrder = result.content.sortedByDescending { it.title }.map { it.id }
		assertEquals(expectedOrder, result.content.map { it.id })
	}
}
