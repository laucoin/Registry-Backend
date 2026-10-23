package fr.laucoin.registry.backend.infrastructure.driving.api.filter

import io.netty.buffer.UnpooledByteBufAllocator
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.core.io.buffer.NettyDataBuffer
import org.springframework.core.io.buffer.NettyDataBufferFactory
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tools.jackson.databind.json.JsonMapper

class ETagResponseFilterTest {
	private val filter = ETagResponseFilter(JsonMapper.builder().build())
	private val itemPath = "/api/v2/projects/1/activities/9f1c2c1e-6f35-4b6e-8f1a-0a3f4f9d2a10"

	private companion object {
		@JvmStatic
		fun `Should set the ETag only on single resource responses`(): Stream<Arguments> = Stream.of(
			Arguments.of("GET", "/api/v2/projects/1/activities/9f1c2c1e-6f35-4b6e-8f1a-0a3f4f9d2a10", null, """{"lastEdition":{"dateTime":"2026-10-13T10:02:11.123Z"}}""", "\"1791885731123\""),
			Arguments.of("GET", "/api/v2/projects/1/activities/9f1c2c1e-6f35-4b6e-8f1a-0a3f4f9d2a10", OK, """{"lastEdition":{"dateTime":"2026-10-13T10:02:11.123456Z"}}""", "\"1791885731123\""),
			Arguments.of("PATCH", "/api/v2/projects/1/activities/9f1c2c1e-6f35-4b6e-8f1a-0a3f4f9d2a10", OK, """{"lastEdition":{"dateTime":"2026-10-13T12:02:11.123+02:00"}}""", "\"1791885731123\""),
			Arguments.of("POST", "/api/v2/projects/1/activities", HttpStatus.CREATED, """{"lastEdition":{"dateTime":"2026-10-13T10:02:11.123Z"}}""", null),
			Arguments.of("POST", "/api/v2/projects/1/activities/9f1c2c1e-6f35-4b6e-8f1a-0a3f4f9d2a10/disable", OK, """{"lastEdition":{"dateTime":"2026-10-13T10:02:11.123Z"}}""", "\"1791885731123\""),
			Arguments.of("POST", "/api/v2/projects/1/groups/9f1c2c1e-6f35-4b6e-8f1a-0a3f4f9d2a10/members", OK, """{"lastEdition":{"dateTime":"2026-10-13T10:02:11.123Z"}}""", null),
			Arguments.of("PATCH", "/api/v2/projects/1/activities/9f1c2c1e-6f35-4b6e-8f1a-0a3f4f9d2a10", OK, """{"lastEdition":{"dateTime":1791885731.123456000}}""", "\"1791885731123\""),
			Arguments.of("PATCH", "/api/v2/projects/1/activities/9f1c2c1e-6f35-4b6e-8f1a-0a3f4f9d2a10", OK, """{"lastEdition":{"dateTime":"not a date"}}""", null),
			Arguments.of("PATCH", "/api/v2/projects/1/activities/9f1c2c1e-6f35-4b6e-8f1a-0a3f4f9d2a10", OK, """{"lastEdition":{"dateTime":99999999999999999999.5}}""", null),
			Arguments.of("PATCH", "/api/v2/projects/1/activities/9f1c2c1e-6f35-4b6e-8f1a-0a3f4f9d2a10", OK, """{"lastEdition":{"dateTime":"+999999999-12-31T23:59:59Z"}}""", null),
			Arguments.of("GET", "/api/v2/projects/1/activities", OK, """{"lastEdition":{"dateTime":"2026-10-13T10:02:11.123Z"}}""", null),
			Arguments.of("GET", "/api/v2/projects/1/activities/9f1c2c1e-6f35-4b6e-8f1a-0a3f4f9d2a10", OK, """{"name":"no edition"}""", null),
			Arguments.of("GET", "/api/v2/projects/1/activities/9f1c2c1e-6f35-4b6e-8f1a-0a3f4f9d2a10", OK, "not json", null),
			Arguments.of("GET", "/api/v2/projects/1/activities/9f1c2c1e-6f35-4b6e-8f1a-0a3f4f9d2a10", BAD_REQUEST, """{"lastEdition":{"dateTime":"2026-10-13T10:02:11.123Z"}}""", null),
			Arguments.of("GET", "/other/9f1c2c1e-6f35-4b6e-8f1a-0a3f4f9d2a10", OK, """{"lastEdition":{"dateTime":"2026-10-13T10:02:11.123Z"}}""", null),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should set the ETag only on single resource responses`(
		method: String,
		path: String,
		status: HttpStatus?,
		body: String,
		expectedETag: String?,
	) {
		// Arrange
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.valueOf(method), path))
		val chain = WebFilterChain {
			status?.let { code -> it.response.statusCode = code }
			it.response.headers.contentType = APPLICATION_JSON
			it.response.writeWith(Mono.just(it.response.bufferFactory().wrap(body.toByteArray()))).then(it.response.setComplete())
		}

		// Act
		filter.filter(exchange, chain).block()

		// Assert
		assertEquals(expectedETag, exchange.response.headers.eTag)
	}

	@Test
	fun `Should leave non JSON responses untouched`() {
		// Arrange
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get(itemPath))
		val chain = WebFilterChain {
			it.response.headers.contentType = MediaType.TEXT_PLAIN
			it.response.writeWith(Mono.just(it.response.bufferFactory().wrap("""{"lastEdition":{"dateTime":"2026-10-13T10:02:11Z"}}""".toByteArray()))).then(it.response.setComplete())
		}

		// Act
		filter.filter(exchange, chain).block()

		// Assert
		assertNull(exchange.response.headers.eTag)
	}

	@Test
	fun `Should write the body unchanged when it is split in several buffers`() {
		// Arrange
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get(itemPath))
		val parts = listOf("""{"name":"Hike","last""", """Edition":{"dateTime":"2026-10-13T10:02:11.123Z"}}""")
		val chain = WebFilterChain {
			it.response.headers.contentType = APPLICATION_JSON
			it.response.writeWith(Flux.fromIterable(parts).map { part -> it.response.bufferFactory().wrap(part.toByteArray()) })
		}

		// Act
		filter.filter(exchange, chain).block()

		// Assert
		assertEquals(parts.joinToString(""), exchange.response.bodyAsString.block())
		assertEquals("\"1791885731123\"", exchange.response.headers.eTag)
	}

	@Test
	fun `Should write an empty body unchanged`() {
		// Arrange
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.patch(itemPath))
		val chain = WebFilterChain {
			it.response.headers.contentType = APPLICATION_JSON
			it.response.writeWith(Mono.empty())
		}

		// Act
		filter.filter(exchange, chain).block()

		// Assert
		assertNull(exchange.response.headers.eTag)
	}

	@Test
	fun `Should write a body over the parsing cap unchanged and without ETag`() {
		// Arrange
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get(itemPath))
		val padding = "x".repeat(1_100_000)
		val body = """{"padding":"$padding","lastEdition":{"dateTime":"2026-10-13T10:02:11.123Z"}}"""
		val chain = WebFilterChain {
			it.response.headers.contentType = APPLICATION_JSON
			it.response.writeWith(Mono.just(it.response.bufferFactory().wrap(body.toByteArray())))
		}

		// Act
		filter.filter(exchange, chain).block()

		// Assert
		assertEquals(body, exchange.response.bodyAsString.block())
		assertNull(exchange.response.headers.eTag)
	}

	@Test
	fun `Should compute the ETag of a body of exactly one mebibyte and skip one byte more`() {
		// Arrange
		val skeleton = """{"padding":"","lastEdition":{"dateTime":"2026-10-13T10:02:11.123Z"}}"""
		fun bodyOf(size: Int) = skeleton.replace("\"padding\":\"\"", "\"padding\":\"" + "x".repeat(size - skeleton.length) + "\"")
		fun eTagOf(body: String): String? {
			val exchange = MockServerWebExchange.from(MockServerHttpRequest.get(itemPath))
			val chain = WebFilterChain {
				it.response.headers.contentType = APPLICATION_JSON
				it.response.writeWith(Mono.just(it.response.bufferFactory().wrap(body.toByteArray())))
			}
			filter.filter(exchange, chain).block()
			return exchange.response.headers.eTag
		}

		// Act
		val atCap = eTagOf(bodyOf(1_048_576))
		val overCap = eTagOf(bodyOf(1_048_577))

		// Assert
		assertEquals("\"1791885731123\"", atCap)
		assertNull(overCap)
	}

	@Test
	fun `Should propagate a failure of the body without an ETag`() {
		// Arrange
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get(itemPath))
		val chain = WebFilterChain {
			it.response.headers.contentType = APPLICATION_JSON
			it.response.writeWith(
				Flux.concat(
					Mono.just(it.response.bufferFactory().wrap("""{"name":""".toByteArray())),
					Flux.error(IllegalStateException("connection reset")),
				),
			)
		}

		// Act
		val failure = assertThrows(IllegalStateException::class.java) { filter.filter(exchange, chain).block() }

		// Assert
		assertEquals("connection reset", failure.message)
		assertNull(exchange.response.headers.eTag)
	}

	@Test
	fun `Should write an oversized body split in several buffers unchanged and without ETag`() {
		// Arrange
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get(itemPath))
		val parts = listOf("""{"padding":"""", "x".repeat(1_048_577), """","lastEdition":{"dateTime":"2026-10-13T10:02:11.123Z"}}""")
		val chain = WebFilterChain {
			it.response.headers.contentType = APPLICATION_JSON
			it.response.writeWith(Flux.fromIterable(parts).map { part -> it.response.bufferFactory().wrap(part.toByteArray()) })
		}

		// Act
		filter.filter(exchange, chain).block()

		// Assert
		assertEquals(parts.joinToString(""), exchange.response.bodyAsString.block())
		assertNull(exchange.response.headers.eTag)
	}

	@Test
	fun `Should release the buffers already received when the body fails`() {
		// Arrange
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get(itemPath))
		val buffer = NettyDataBufferFactory(UnpooledByteBufAllocator.DEFAULT).wrap("""{"name":""".toByteArray())
		val chain = WebFilterChain {
			it.response.headers.contentType = APPLICATION_JSON
			it.response.writeWith(Flux.concat(Mono.just(buffer), Flux.error(IllegalStateException("connection reset"))))
		}

		// Act
		assertThrows(IllegalStateException::class.java) { filter.filter(exchange, chain).block() }

		// Assert
		assertEquals(0, (buffer as NettyDataBuffer).nativeBuffer.refCnt())
	}
}
