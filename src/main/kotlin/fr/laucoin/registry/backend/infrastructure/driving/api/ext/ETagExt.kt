package fr.laucoin.registry.backend.infrastructure.driving.api.ext

import java.time.Instant
import org.springframework.web.server.ServerWebExchange

/**
 * Purpose: Reads the optimistic concurrency precondition carried by the If-Match request header.
 * Scope: Owns the ETag format (last edition instant, in milliseconds) and the parsing of If-Match into expected instants.
 * Limits: Only parses the header; the comparison and the locking belong to the concurrency service.
 */
object ETagExt {
	private const val ANY = "*"
	private const val WEAK_PREFIX = "W/"

	fun Instant.toETag(): String = "\"${toEpochMilli()}\""

	fun ServerWebExchange.expectedLastEditions(): List<Instant>? {
		val expected = request.headers.ifMatch.map { it.removePrefix(WEAK_PREFIX) }
		if (expected.isEmpty() || ANY in expected) return null

		return expected.mapNotNull { it.removeSurrounding("\"").toLongOrNull()?.let(Instant::ofEpochMilli) }
	}
}
