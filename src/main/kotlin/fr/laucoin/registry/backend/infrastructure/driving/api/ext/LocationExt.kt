package fr.laucoin.registry.backend.infrastructure.driving.api.ext

import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.GenericReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.ETagExt.toETag
import java.net.URI
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.ResponseEntity
import org.springframework.web.server.ServerWebExchange

/**
 * Purpose: Builds the 201 response of a creation endpoint, with the Location and ETag headers of the new resource.
 * Scope: Owns the relative Location computation from the request path and the created resource identifier.
 * Limits: Never builds an absolute URI, since the public host is only known by the reverse proxy.
 */
object LocationExt {
	fun <T: GenericReaderDto> ServerWebExchange.created(body: T, collectionSuffix: String = ""): ResponseEntity<T> {
		val id = body.id ?: return ResponseEntity.status(CREATED).body(body)
		val collectionPath = request.path.value().trimEnd('/').removeSuffix(collectionSuffix)

		val response = ResponseEntity.created(URI.create("$collectionPath/$id"))
		body.lastEdition?.dateTime?.let { response.eTag(it.toInstant().toETag()) }

		return response.body(body)
	}
}
