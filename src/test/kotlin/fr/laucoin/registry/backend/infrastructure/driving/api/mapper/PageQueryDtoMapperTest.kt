package fr.laucoin.registry.backend.infrastructure.driving.api.mapper

import fr.laucoin.registry.backend.infrastructure.driving.api.dto.PageQueryDto
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class PageQueryDtoMapperTest {
	private val mapper = PageQueryDtoMapper()

	@Test
	fun `Should toPageable compute offset from page and size`() {
		val query = PageQueryDto().apply {
			page = 3
			size = 20
		}

		val result = mapper.toPageable(query)

		assertEquals(60, result.offset)
		assertEquals(20, result.limit)
	}

	@Test
	fun `Should toPageable default to the first page`() {
		val result = mapper.toPageable(PageQueryDto())

		assertEquals(0, result.offset)
		assertEquals(20, result.limit)
	}
}
