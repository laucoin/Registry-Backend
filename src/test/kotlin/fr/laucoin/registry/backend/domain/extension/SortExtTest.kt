package fr.laucoin.registry.backend.domain.extension

import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_NOT_SUPPORTED
import fr.laucoin.registry.backend.domain.extension.SortExt.toSortModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.SortModel
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpStatus.BAD_REQUEST

class SortExtTest {
	private val allowed = setOf("name", "startDate")

	private companion object {
		@JvmStatic
		fun `Should parse the sort value`(): Stream<Arguments> = Stream.of(
			Arguments.of(null, null),
			Arguments.of("", null),
			Arguments.of("   ", null),
			Arguments.of("name", SortModel("name", false)),
			Arguments.of(" -startDate ", SortModel("startDate", true)),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should parse the sort value`(raw: String?, expected: SortModel?) {
		// Act
		val result = raw.toSortModel(allowed)

		// Assert
		assertEquals(expected, result)
	}

	@Test
	fun `Should return nothing without sort value`() {
		// Act
		val result = (null as String?).toSortModel(allowed)

		// Assert
		assertNull(result)
	}

	@Test
	fun `Should reject a field outside the allow-list`() {
		// Act
		val exception = assertThrows(RegistryException::class.java) { "-password".toSortModel(allowed) }

		// Assert
		assertEquals(BAD_REQUEST, exception.status)
		assertEquals(SORT_FIELD_NOT_SUPPORTED, exception.code)
		assertEquals(arrayListOf<Any?>("password", "name, startDate"), exception.args)
	}

	@Test
	fun `Should echo only a short and clean version of an unsupported field`() {
		// Arrange
		val raw = "-" + "a".repeat(40) + "\n2026 INFO forged"

		// Act
		val exception = assertThrows(RegistryException::class.java) { raw.toSortModel(allowed) }

		// Assert
		assertEquals(arrayListOf<Any?>("a".repeat(32), "name, startDate"), exception.args)
	}

	@Test
	fun `Should strip control characters from the echoed field`() {
		// Arrange
		val raw = "-ab\ncd\u0000e"

		// Act
		val exception = assertThrows(RegistryException::class.java) { raw.toSortModel(allowed) }

		// Assert
		assertEquals(arrayListOf<Any?>("abcde", "name, startDate"), exception.args)
	}

	@Test
	fun `Should keep the underscores of the echoed field`() {
		// Arrange
		val raw = "start_date"

		// Act
		val exception = assertThrows(RegistryException::class.java) { raw.toSortModel(allowed) }

		// Assert
		assertEquals(arrayListOf<Any?>("start_date", "name, startDate"), exception.args)
	}
}
