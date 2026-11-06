package fr.laucoin.registry.backend.config

import fr.laucoin.registry.backend.infrastructure.driving.security.chain.SecurityHeadersChainCustomizer
import java.net.URI
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.header.ContentSecurityPolicyServerHttpHeadersWriter.CONTENT_SECURITY_POLICY
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult
import org.springframework.util.AntPathMatcher
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Objectif : Définit la chaîne de sécurité de la documentation Swagger/OpenAPI, servie sur le port de supervision.
 * Périmètre : Accès libre en lecture à la documentation, en-têtes de sécurité et CSP dédiée autorisant l'IDP et l'API.
 * Limites : Ne protège pas l'API ; la chaîne principale s'en charge.
 */
@Configuration
class DocumentationSecurityConfig(
	private val securityHeaders: SecurityHeadersChainCustomizer,
	@param:Value($$"${registry.server.management-port}")
	private val managementPort: Int,
	@param:Value($$"${registry.server.port}")
	private val apiPort: Int,
	@param:Value($$"${external.idp.token-uri}")
	private val idpTokenUri: String,
) {
	@Bean
	@Order(1)
	fun documentationSecurityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
		val protectedHttp = http
			.securityMatcher(documentationMatcher())
			.authorizeExchange { it.anyExchange().permitAll() }

		return securityHeaders.configure(protectedHttp, null)
			.documentationContentSecurityPolicy()
			// codeql[java/spring-disabled-csrf-protection] : cette chaîne ne sert que la documentation Swagger/OpenAPI
			// statique (GET uniquement, permitAll, aucune requête modifiant l'état ne l'atteint) — le CSRF ne
			// s'applique pas ici, selon le même raisonnement que celui qui exempte déjà les méthodes HTTP sûres sur la chaîne principale.
			.csrf { it.disable() }
			.formLogin { it.disable() }
			.logout { it.disable() }
			.build()
	}

	private fun ServerHttpSecurity.documentationContentSecurityPolicy() = headers { headerSpec ->
		headerSpec.writer { exchange ->
			exchange.response.headers.set(CONTENT_SECURITY_POLICY, documentationContentSecurityPolicy(exchange))
			Mono.empty()
		}
	}

	private fun documentationContentSecurityPolicy(exchange: ServerWebExchange): String {
		val idpOrigin = URI(idpTokenUri).let { "${it.scheme}://${it.authority}" }
		val apiOrigin = exchange.request.headers.host?.hostName
			?.let { apiOrigin(exchange.request.uri.scheme, it, apiPort) }
		return documentationContentSecurityPolicy(idpOrigin, apiOrigin)
	}

	private fun documentationMatcher() = ServerWebExchangeMatcher { exchange ->
		val isManagementPort = exchange.request.localAddress?.port == managementPort
		val isDocumentationPath = DOCUMENTATION_PATHS.any { pathMatcher.match(it, exchange.request.path.value()) }
		if (isManagementPort && isDocumentationPath) MatchResult.match() else MatchResult.notMatch()
	}

	internal companion object {
		private val pathMatcher = AntPathMatcher()
		private val DOCUMENTATION_PATHS = listOf("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
		private val HOST_NAME_PATTERN = Regex("[A-Za-z0-9.\\-\\[\\]:]+")

		internal fun apiOrigin(scheme: String, hostName: String, apiPort: Int): String? {
			return if (HOST_NAME_PATTERN.matches(hostName)) "$scheme://$hostName:$apiPort" else null
		}

		internal fun documentationContentSecurityPolicy(idpOrigin: String, apiOrigin: String?): String {
			val connectSources = listOfNotNull("'self'", idpOrigin, apiOrigin).joinToString(" ")
			return "default-src 'self'; connect-src $connectSources; frame-ancestors 'none'"
		}
	}
}
