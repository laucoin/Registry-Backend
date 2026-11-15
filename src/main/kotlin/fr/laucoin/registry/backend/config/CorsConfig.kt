package fr.laucoin.registry.backend.config

import fr.laucoin.registry.backend.infrastructure.driving.security.service.CsrfTokenService.Companion.HEADER_NAME
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS
import org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN
import org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.CACHE_CONTROL
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpHeaders.ETAG
import org.springframework.http.HttpHeaders.IF_MATCH
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.http.HttpMethod.DELETE
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.HEAD
import org.springframework.http.HttpMethod.OPTIONS
import org.springframework.http.HttpMethod.PATCH
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpMethod.PUT
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

/**
 * Objectif : Définit la politique CORS de l'API sous forme de liste blanche explicite d'origines.
 * Périmètre : Origines autorisées par la configuration, méthodes, en-têtes acceptés et en-têtes exposés.
 * Limites : N'accepte jamais « * » ; ne porte aucune autorisation métier.
 */
@Configuration
class CorsConfig(
	@param:Value($$"${external.cors.urls}")
	private val corsUrls: List<String>,
) {
	@Bean
	fun corsConfigurationSource(): CorsConfigurationSource {
		val configuration = CorsConfiguration()
		configuration.allowedOrigins = corsUrls
		configuration.allowedMethods = CORS_ALLOWED_METHODS
		configuration.allowCredentials = true
		configuration.allowedHeaders = CORS_ALLOWED_HEADERS
		configuration.exposedHeaders = listOf(HEADER_NAME, ETAG, LOCATION)
		val source = UrlBasedCorsConfigurationSource()
		source.registerCorsConfiguration("/**", configuration)
		return source
	}

	private companion object {
		val CORS_ALLOWED_METHODS = listOf(
			HEAD.name(),
			GET.name(),
			POST.name(),
			PUT.name(),
			DELETE.name(),
			PATCH.name(),
			OPTIONS.name(),
		)

		val CORS_ALLOWED_HEADERS = listOf(
			AUTHORIZATION,
			CACHE_CONTROL,
			CONTENT_TYPE,
			ACCEPT_LANGUAGE,
			ACCESS_CONTROL_ALLOW_ORIGIN,
			ACCESS_CONTROL_ALLOW_HEADERS,
			ACCESS_CONTROL_EXPOSE_HEADERS,
			IF_MATCH,
			HEADER_NAME,
		)
	}
}
