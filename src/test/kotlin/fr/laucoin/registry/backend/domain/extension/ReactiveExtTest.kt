package fr.laucoin.registry.backend.domain.extension

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.RegistryException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpStatus.NOT_FOUND
import reactor.core.Exceptions
import reactor.core.publisher.Mono
import java.util.UUID
import java.util.stream.Stream

class ReactiveExtTest {

	private companion object {
		@JvmStatic
		fun `Should notFoundIfEmpty throw a 404`(): Stream<Arguments> = Stream.of(
			Arguments.of(Mono.empty<String>()),
			Arguments.of(Mono.empty<Int>()),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun <T : Any> `Should notFoundIfEmpty throw a 404`(value: Mono<T>) {
		// Act
		val id = UUID.randomUUID()
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			value.notFoundIfEmpty(id).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(id.toString(), result.args?.first())
		assertEquals(1, result.args?.size)
		assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
	}
}
