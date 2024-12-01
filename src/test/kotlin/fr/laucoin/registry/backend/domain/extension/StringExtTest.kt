package fr.laucoin.registry.backend.domain.extension

import fr.laucoin.registry.backend.domain.extension.StringExt.generateRandomString
import fr.laucoin.registry.backend.domain.extension.StringExt.getStringBetween
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class StringExtTest {
    companion object {
        @JvmStatic
        fun `Should getStringBetween return String between`(): Stream<Arguments> = Stream.of(
            Arguments.of(null, "", null),
            Arguments.of("\"test", "\"", null),
            Arguments.of("test\"", "\"", null),
            Arguments.of("\"test\"", "\"", "test"),
            Arguments.of("\"", "\"", null),
        )
    }

    @ParameterizedTest
    @MethodSource
    fun `Should getStringBetween return String between`(value: String?, delimiter: String, expected: String?) {
        // Arrange
        // Act
        val result = value.getStringBetween(delimiter)

        // Assert
        assertEquals(expected, result)
    }

    @Test
    fun `Should generateRandomString return random string`() {
        // Arrange
        val expectedLength = 10

        // Act
        val result = generateRandomString()

        // Assert
        assertEquals(expectedLength, result.length)
    }
}
