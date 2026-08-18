package fr.laucoin.registry.backend.domain.handler

import fr.laucoin.registry.backend.domain.constant.AuditConst.CORRELATION_ID_HEADER
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

/**
 * Every response carries exactly one correlation id:
 * the caller's when well-formed, a generated one otherwise. The handler runs
 * at highest precedence, so even requests rejected by the security chain are
 * covered — no authentication needed here.
 */
class CorrelationIdHandlerTest : TestContext() {
	@Autowired
	private lateinit var webClient: WebTestClient

	companion object {
		private val WELL_FORMED = Regex("^[A-Za-z0-9-]{8,64}$")

		@JvmStatic
		fun malformedIdsProvider(): Stream<Arguments> = Stream.of(
			arguments("x"),
			arguments("a".repeat(65)),
			arguments("malformed_correlation_id"),
			arguments("malformed correlation id!"),
		)
	}

	@Test
	fun `Should generate a well-formed correlation id when the header is absent`() {
		// Act
		val correlationId = webClient
			.get()
			.uri(uriBuilder("/api/v2/users/{id}", listOf(UUID.randomUUID()), emptyList()))
			.exchange()
			.returnResult(String::class.java)
			.responseHeaders.getFirst(CORRELATION_ID_HEADER)

		// Assert
		assertNotNull(correlationId)
		assert(WELL_FORMED.matches(correlationId))
	}

	@ParameterizedTest
	@MethodSource("malformedIdsProvider")
	fun `Should replace a malformed correlation id with a generated one`(malformedId: String) {
		// Act
		val correlationId = webClient
			.get()
			.uri(uriBuilder("/api/v2/users/{id}", listOf(UUID.randomUUID()), emptyList()))
			.header(CORRELATION_ID_HEADER, malformedId)
			.exchange()
			.returnResult(String::class.java)
			.responseHeaders.getFirst(CORRELATION_ID_HEADER)

		// Assert
		assertNotNull(correlationId)
		assertNotEquals(malformedId, correlationId)
		assert(WELL_FORMED.matches(correlationId))
	}
}
