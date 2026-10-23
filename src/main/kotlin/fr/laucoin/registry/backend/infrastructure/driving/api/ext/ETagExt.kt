package fr.laucoin.registry.backend.infrastructure.driving.api.ext

import fr.laucoin.registry.backend.domain.constant.ErrorConst.RESOURCE_MODIFIED_SINCE_READ
import fr.laucoin.registry.backend.domain.model.GenericModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import java.time.Instant
import java.util.Optional
import org.springframework.http.HttpStatus.PRECONDITION_FAILED
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Purpose: Provides the optimistic concurrency check driven by the If-Match request header.
 * Scope: Owns the ETag format (last edition instant, in milliseconds) and the comparison with If-Match.
 * Limits: The check is optional (no If-Match, no check) and not atomic with the update that follows it.
 */
object ETagExt {
	private const val ANY = "*"
	private const val WEAK_PREFIX = "W/"

	fun Instant.toETag(): String = "\"${toEpochMilli()}\""

	fun <M : GenericModel, R : Any> ServerWebExchange.ifUnchanged(
		current: () -> Mono<M>,
		action: () -> Mono<R>,
	): Mono<R> {
		val expected = request.headers.ifMatch.map { it.removePrefix(WEAK_PREFIX) }
		if (expected.isEmpty() || ANY in expected) return action()

		return Mono.defer(current)
			.map { Optional.of(it) }
			.defaultIfEmpty(Optional.empty())
			.flatMap { checkThenAct(it, expected, action) }
	}

	private fun <M : GenericModel, R : Any> checkThenAct(
		current: Optional<M>,
		expected: List<String>,
		action: () -> Mono<R>,
	): Mono<R> {
		val etag = current.map { it.lastEdition?.dateTime?.toInstant()?.toETag() }.orElse(null)

		return if (etag != null && etag !in expected) {
			Mono.error(RegistryException(PRECONDITION_FAILED, RESOURCE_MODIFIED_SINCE_READ))
		} else {
			action()
		}
	}
}
