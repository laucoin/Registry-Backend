package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.PageModel
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class PageReaderDtoMapperTest {
	private val mapper = PageReaderDtoMapper()

	@Test
	fun `Should toDto map every field and apply the item mapper to the content`() {
		val lastRefresh = ZonedDateTime.now()
		val page = PageModel(
			pageNumber = 1,
			pageSize = 20,
			totalPages = 3,
			totalElements = 42,
			content = listOf(1, 2, 3),
			lastRefresh = lastRefresh,
		)

		val result = mapper.toDto(page) { it * 10 }

		assertEquals(1, result.pageNumber)
		assertEquals(20, result.pageSize)
		assertEquals(3, result.totalPages)
		assertEquals(42, result.totalElements)
		assertEquals(listOf(10, 20, 30), result.content)
		assertEquals(lastRefresh, result.lastRefresh)
	}
}
