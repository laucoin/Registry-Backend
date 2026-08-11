package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository

import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.orderByWithRelevance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * A TEXT SEARCH is answered by relevance first — the caller's criterion becomes
 * the tie-break. Without it a v2 sorted page fell back to the plain
 * alphabetical ordering, so the closest match could sit pages away while the
 * first page held whatever happened to start with an "A".
 */
class GenericQueriesTest {
	private companion object {
		private const val SORT = "t.last_name ASC"

		@JvmStatic
		fun `Should lead with relevance only while a text search is active`(): Stream<Arguments> = Stream.of(
			Arguments.of("nova", "similarity_score DESC, $SORT, t.id ASC"),
			Arguments.of(null, "$SORT, t.id ASC"),
			Arguments.of("", "$SORT, t.id ASC"),
			Arguments.of("   ", "$SORT, t.id ASC"),
		)

		@JvmStatic
		fun `Should always close the ordering with the id`(): Stream<Arguments> = Stream.of(
			Arguments.of("nova"),
			Arguments.of(null),
			Arguments.of(""),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should lead with relevance only while a text search is active`(
		textSearched: String?,
		expected: String,
	) {
		// Act
		val orderBy = orderByWithRelevance(textSearched, SORT)

		// Assert
		assertEquals(expected, orderBy)
	}

	/**
	 * The id always closes the ordering: without a total order two rows that tie
	 * on every other column can swap between pages, and a paging client sees one
	 * of them twice while never seeing the other.
	 */
	@ParameterizedTest
	@MethodSource
	fun `Should always close the ordering with the id`(textSearched: String?) {
		// Act
		val orderBy = orderByWithRelevance(textSearched, SORT)

		// Assert
		assertTrue(orderBy.endsWith("t.id ASC"))
	}
}
