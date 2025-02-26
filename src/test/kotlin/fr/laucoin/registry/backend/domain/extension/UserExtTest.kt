package fr.laucoin.registry.backend.domain.extension

import fr.laucoin.registry.backend.domain.extension.UserExt.getClaimAsUUID
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.oauth2.jwt.Jwt

class UserExtTest {

    companion object {
        @JvmStatic
        fun `Should getClaimAsUUID return UUID`(): Stream<Arguments> = Stream.of(
            Arguments.of(false, "", null),
            Arguments.of(true, "wrongFormat", null),
            Arguments.of(true, "123e4567-e89b-12d3-a456-426614174000", "123e4567-e89b-12d3-a456-426614174000"),
        )
    }

    @ParameterizedTest
    @MethodSource
    fun `Should getClaimAsUUID return UUID`(
        asKey: Boolean,
        uuid: String,
        expected: String?,
    ) {
        // Arrange
        val key = "uuid"
        val jwt: Jwt = mock()
        whenever(jwt.hasClaim(key)).thenReturn(asKey)
        whenever(jwt.getClaimAsString(key)).thenReturn(uuid)

        // Act
        val result = jwt.getClaimAsUUID(key)

        // Assert
        assertEquals(expected, result?.toString())
    }
}
