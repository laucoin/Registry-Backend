package fr.laucoin.registry.backend.infrastructure.driving.api.ext

import java.time.Instant
import org.springframework.web.server.ServerWebExchange

/**
 * Objectif : Lit la précondition de concurrence optimiste portée par l'en-tête de requête If-Match.
 * Périmètre : Porte le format de l'ETag (instant de dernière édition, en millisecondes) et l'analyse de If-Match en instants attendus.
 * Limites : Ne fait qu'analyser l'en-tête ; la comparaison et le verrouillage relèvent du service de concurrence.
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
