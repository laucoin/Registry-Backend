package fr.laucoin.registry.backend.infrastructure.driving.api.ext

import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.GenericReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.ETagExt.toETag
import java.net.URI
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.ResponseEntity
import org.springframework.web.server.ServerWebExchange

/**
 * Objectif : Construit la réponse 201 d'un endpoint de création, avec les en-têtes Location et ETag de la nouvelle ressource.
 * Périmètre : Porte le calcul de la Location relative à partir du chemin de la requête et de l'identifiant de la ressource créée.
 * Limites : Ne construit jamais d'URI absolue, l'hôte public n'étant connu que du reverse proxy.
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
