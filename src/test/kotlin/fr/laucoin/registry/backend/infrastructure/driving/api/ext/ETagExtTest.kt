package fr.laucoin.registry.backend.infrastructure.driving.api.ext

import fr.laucoin.registry.backend.domain.constant.ErrorConst.RESOURCE_MODIFIED_SINCE_READ
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.ETagExt.ifUnchanged
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.ETagExt.toETag
import java.time.Instant
import java.time.ZonedDateTime
import java.time.ZonedDateTime.parse
import java.util.stream.Stream
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpHeaders.IF_MATCH
import org.springframework.http.HttpStatus.PRECONDITION_FAILED
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class ETagExtTest {
	private companion object {
		private val edition: ZonedDateTime = parse("2026-10-13T10:02:11.123456Z")
		private val currentETag = edition.toInstant().toETag()

		@JvmStatic
		fun `Should run the action when the precondition holds`(): Stream<Arguments> = Stream.of(
			Arguments.of(null),
			Arguments.of("*"),
			Arguments.of(currentETag),
			Arguments.of("W/$currentETag"),
			Arguments.of("\"1\", $currentETag"),
		)
	}

	private fun exchange(ifMatch: String?) = MockServerWebExchange.from(
		MockServerHttpRequest.patch("/api/v2/anything").also { builder -> ifMatch?.let { builder.header(IF_MATCH, it) } },
	)

	private fun model() = ActivityModel().apply { lastEdition = HistoryModel(dateTime = edition) }

	@Test
	fun `Should format the ETag with the millisecond of the instant`() {
		// Act
		val result = Instant.parse("2026-10-13T10:02:11.123456Z").toETag()

		// Assert
		assertEquals("\"1791885731123\"", result)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should run the action when the precondition holds`(ifMatch: String?) {
		// Act
		val result = exchange(ifMatch).ifUnchanged(current = { Mono.just(model()) }, action = { Mono.just("done") })

		// Assert
		StepVerifier.create(result).expectNext("done").verifyComplete()
	}

	@Test
	fun `Should reject the action when the resource changed since it was read`() {
		// Act
		val result = exchange("\"1\"").ifUnchanged(current = { Mono.just(model()) }, action = { Mono.just("done") })

		// Assert
		StepVerifier.create(result)
			.expectErrorSatisfies {
				assertEquals(PRECONDITION_FAILED, (it as RegistryException).status)
				assertEquals(RESOURCE_MODIFIED_SINCE_READ, it.code)
			}
			.verify()
	}

	@Test
	fun `Should let the action report a missing resource`() {
		// Act
		val result = exchange("\"1\"").ifUnchanged(
			current = { Mono.empty<ActivityModel>() },
			action = { Mono.error<String>(IllegalStateException("not found")) },
		)

		// Assert
		StepVerifier.create(result).expectError(IllegalStateException::class.java).verify()
	}
}
