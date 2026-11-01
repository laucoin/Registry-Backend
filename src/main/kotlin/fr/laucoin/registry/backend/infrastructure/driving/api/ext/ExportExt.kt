package fr.laucoin.registry.backend.infrastructure.driving.api.ext

import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders.CACHE_CONTROL
import org.springframework.http.HttpHeaders.CONTENT_DISPOSITION
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.ResponseEntity

/**
 * Objectif : Construit la réponse de téléchargement JSON d'un export de données personnelles.
 * Périmètre : Porte le type de contenu, le nom de fichier (Content-Disposition) et l'interdiction de mise en cache (no-store).
 * Limites : Ne génère ni ne mappe le contenu de l'export ; le corps est fourni par l'appelant.
 */
object ExportExt {
	fun <T : Any> T.asAttachment(filename: String): ResponseEntity<T> {
		return ResponseEntity.ok()
			.contentType(APPLICATION_JSON)
			.header(CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
			.header(CACHE_CONTROL, "no-store")
			.body(this)
	}
}
