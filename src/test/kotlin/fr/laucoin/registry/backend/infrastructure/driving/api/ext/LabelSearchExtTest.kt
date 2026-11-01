package fr.laucoin.registry.backend.infrastructure.driving.api.ext

import fr.laucoin.registry.backend.infrastructure.driving.api.ext.LabelSearchExt.matchesQuery
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class LabelSearchExtTest {
	@Test
	fun `Should matchesQuery accept every label without a query`() {
		// Act
		val result = "Medical appointment".matchesQuery(null)

		// Assert
		assertTrue(result)
	}

	@Test
	fun `Should matchesQuery accept a label close to the query`() {
		// Act
		val result = "Medical appointment".matchesQuery("medic")

		// Assert
		assertTrue(result)
	}

	@Test
	fun `Should matchesQuery refuse a label sharing nothing with the query`() {
		// Act
		val result = "abc".matchesQuery("xyz")

		// Assert
		assertFalse(result)
	}
}
