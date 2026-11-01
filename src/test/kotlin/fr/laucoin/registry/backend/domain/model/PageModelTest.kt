package fr.laucoin.registry.backend.domain.model

import java.util.stream.Stream
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class PageModelTest {
	private companion object {
		@JvmStatic
		fun `Should tell whether another page follows`(): Stream<Arguments> = Stream.of(
			Arguments.of(0, 20, 0L, false),
			Arguments.of(0, 20, 20L, false),
			Arguments.of(0, 20, 21L, true),
			Arguments.of(1, 20, 21L, false),
			Arguments.of(1, 20, 41L, true),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should tell whether another page follows`(page: Int, size: Int, totalElements: Long, expected: Boolean) {
		// Arrange
		val model = PageModel<String>(PageableModel(page.toLong() * size, size), totalElements, emptyList())

		// Act
		val result = model.hasNext

		// Assert
		assertEquals(expected, result)
	}
}
