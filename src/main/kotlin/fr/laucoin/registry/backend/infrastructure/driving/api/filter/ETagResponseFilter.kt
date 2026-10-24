package fr.laucoin.registry.backend.infrastructure.driving.api.filter

import fr.laucoin.registry.backend.infrastructure.driving.api.ext.ETagExt.toETag
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.time.DateTimeException
import java.time.Instant
import java.time.OffsetDateTime
import org.reactivestreams.Publisher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered.LOWEST_PRECEDENCE
import org.springframework.core.annotation.Order
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.PATCH
import org.springframework.http.HttpMethod.POST
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

/**
 * Objectif : Ajoute un en-tête ETag aux réponses JSON qui renvoient une seule ressource.
 * Périmètre : Dérive l'ETag de la date-heure lastEdition du corps d'une lecture unitaire, d'une mise à jour ou d'une transition d'état.
 * Limites : Les créations posent leur propre ETag ; les listes, les corps sans lastEdition et les corps de plus de 1 Mio sont laissés intacts, et il ne répond jamais 304.
 */
@Component
@Order(LOWEST_PRECEDENCE)
class ETagResponseFilter(private val objectMapper: ObjectMapper) : WebFilter {
	private val log: Logger = LoggerFactory.getLogger(ETagResponseFilter::class.java)

	private companion object {
		const val API_PREFIX = "/api/v2/"
		const val MAX_PARSED_BYTES: Int = 1_048_576
		const val UUID = "[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"
		val SINGLE_RESOURCE_PATH = Regex(".*/$UUID$")
		val TRANSITION_PATH = Regex(".*/$UUID/(disable|enable|block|unblock)$")
	}

	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		return if (appliesTo(exchange)) {
			chain.filter(exchange.mutate().response(ETagResponse(exchange.response)).build())
		} else {
			chain.filter(exchange)
		}
	}

	private fun appliesTo(exchange: ServerWebExchange): Boolean {
		val path = exchange.request.path.value()
		return path.startsWith(API_PREFIX) && when (exchange.request.method) {
			GET -> SINGLE_RESOURCE_PATH.matches(path)
			PATCH -> true
			POST -> TRANSITION_PATH.matches(path)
			else -> false
		}
	}

	private inner class ETagResponse(delegate: ServerHttpResponse) : ServerHttpResponseDecorator(delegate) {
		override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
			if (!isJsonSuccess()) return super.writeWith(body)

			return Flux.from(body)
				.collectList()
				.doOnDiscard(DataBuffer::class.java, DataBufferUtils::release)
				.flatMap(::writeBuffered)
		}

		private fun isJsonSuccess(): Boolean {
			return statusCode?.is2xxSuccessful != false && headers.contentType?.isCompatibleWith(APPLICATION_JSON) == true
		}

		private fun writeBuffered(buffers: List<DataBuffer>): Mono<Void> {
			if (buffers.sumOf { it.readableByteCount() } > MAX_PARSED_BYTES) {
				return super.writeWith(Flux.fromIterable(buffers))
			}
			return writeWithETag(readAndRelease(buffers))
		}

		private fun writeWithETag(bytes: ByteArray): Mono<Void> {
			eTag(bytes)?.let { headers.eTag = it }
			return super.writeWith(Mono.just(bufferFactory().wrap(bytes)))
		}

		private fun readAndRelease(buffers: List<DataBuffer>): ByteArray {
			try {
				val output = ByteArrayOutputStream()
				buffers.forEach { buffer -> output.write(ByteArray(buffer.readableByteCount()).also(buffer::read)) }
				return output.toByteArray()
			} finally {
				buffers.forEach(DataBufferUtils::release)
			}
		}

		private fun eTag(bytes: ByteArray): String? {
			val node = try {
				objectMapper.readTree(bytes)
			} catch (_: JacksonException) {
				log.debug("The body is not a JSON document, no ETag is computed")
				return null
			}
			return toETag(node.path("lastEdition").path("dateTime"))
		}

		private fun toETag(node: JsonNode): String? {
			return try {
				when {
					node.isTextual -> OffsetDateTime.parse(node.asText()).toInstant().toETag()
					node.isNumber -> decimalToInstant(node.decimalValue()).toETag()
					else -> null
				}
			} catch (_: DateTimeException) {
				log.debug("The last edition date-time is not a valid instant, no ETag is computed")
				null
			} catch (_: ArithmeticException) {
				log.debug("The last edition date-time is out of range, no ETag is computed")
				null
			}
		}

		private fun decimalToInstant(seconds: BigDecimal): Instant {
			val epochSeconds = seconds.toBigInteger().longValueExact()
			val nanos = seconds.subtract(BigDecimal(epochSeconds)).movePointRight(9).toInt()
			return Instant.ofEpochSecond(epochSeconds, nanos.toLong())
		}
	}
}
