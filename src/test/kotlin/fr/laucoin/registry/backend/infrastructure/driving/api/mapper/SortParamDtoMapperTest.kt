package fr.laucoin.registry.backend.infrastructure.driving.api.mapper

import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_DIRECTION_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.enumeration.SortDirectionEnum.ASC
import fr.laucoin.registry.backend.domain.enumeration.SortDirectionEnum.DESC
import fr.laucoin.registry.backend.domain.model.RegistryException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.BAD_REQUEST

private enum class TestField { NAME, CREATED_DATE }

class SortParamDtoMapperTest {
	private val mapper = SortParamDtoMapper()

	private fun resolve(name: String): TestField? = TestField.entries.firstOrNull { it.name.equals(name, true) }

	@Test
	fun `Should toSortModels return an empty list when sortParams is null`() {
		val result = mapper.toSortModels<TestField>(null, "ASC", ::resolve)

		assertTrue(result.isEmpty())
	}

	@Test
	fun `Should toSortModels resolve known fields with the given direction`() {
		val result = mapper.toSortModels(listOf("name", "CREATED_DATE"), "DESC", ::resolve)

		assertEquals(2, result.size)
		assertEquals(TestField.NAME, result[0].field)
		assertEquals(DESC, result[0].direction)
		assertEquals(TestField.CREATED_DATE, result[1].field)
		assertEquals(DESC, result[1].direction)
	}

	@Test
	fun `Should toSortModels default to ASC direction`() {
		val result = mapper.toSortModels(listOf("name"), "asc", ::resolve)

		assertEquals(ASC, result.single().direction)
	}

	@Test
	fun `Should toSortModels throw when a sort field is unknown`() {
		val exception = assertFailsWith<RegistryException> {
			mapper.toSortModels(listOf("unknownField"), "ASC", ::resolve)
		}

		assertEquals(BAD_REQUEST, exception.status)
		assertEquals(SORT_FIELD_IS_UNKNOWN, exception.code)
		assertEquals("unknownField", exception.args?.single())
	}

	@Test
	fun `Should toSortModels throw when the direction is unknown`() {
		val exception = assertFailsWith<RegistryException> {
			mapper.toSortModels(listOf("name"), "sideways", ::resolve)
		}

		assertEquals(BAD_REQUEST, exception.status)
		assertEquals(SORT_DIRECTION_IS_UNKNOWN, exception.code)
		assertEquals("sideways", exception.args?.single())
	}
}
