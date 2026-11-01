package fr.laucoin.registry.backend.config

import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.driving.security.handler.AuthenticationRateLimitHandler
import fr.laucoin.registry.backend.infrastructure.driving.security.handler.AuthorizationErrorHandler
import fr.laucoin.registry.backend.infrastructure.driving.security.handler.CookieBearerTokenHandler
import fr.laucoin.registry.backend.infrastructure.driving.security.handler.CsrfTokenHeaderHandler
import fr.laucoin.registry.backend.infrastructure.driving.security.handler.LocaleContextHandler
import fr.laucoin.registry.backend.infrastructure.driving.security.service.CsrfTokenService
import fr.laucoin.registry.backend.infrastructure.driving.security.service.CsrfTokenService.Companion.HEADER_NAME
import fr.laucoin.registry.backend.infrastructure.driving.security.service.PermissionService
import fr.laucoin.registry.backend.infrastructure.driving.security.service.TokenConverterService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
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
import org.springframework.http.HttpMethod.TRACE
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder.FIRST
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler
import org.springframework.security.web.server.header.ContentSecurityPolicyServerHttpHeadersWriter.CONTENT_SECURITY_POLICY
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult
import org.springframework.util.AntPathMatcher
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.i18n.LocaleContextResolver
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Clock
import java.time.Duration
import tools.jackson.databind.ObjectMapper


/**
 * Objectif : Définit les chaînes de filtres de sécurité, la politique CORS et les en-têtes de sécurité.
 * Périmètre : Porte l'authentification, le CSRF, la limitation de débit, les en-têtes de réponse et la liste des endpoints publics.
 * Limites : Ne porte aucune autorisation métier ; la sécurité par méthode sur les contrats décide endpoint par endpoint.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig(
	private val tokenConverter: TokenConverterService,
	private val authorizationErrorHandler: AuthorizationErrorHandler,
	private val localeContextHandler: LocaleContextHandler,
	private val localeContextResolver: LocaleContextResolver,
	private val cookieBearerTokenHandler: CookieBearerTokenHandler,
	private val translateService: ITranslateService,
	private val objectMapper: ObjectMapper,
	private val csrfTokenService: CsrfTokenService,
	private val clock: Clock,
	@param:Value($$"${external.cors.urls}")
	private val corsUrls: List<String>,
	@param:Value($$"${registry.server.management-port}")
	private val managementPort: Int,
	@param:Value($$"${registry.server.port}")
	private val apiPort: Int,
	@param:Value($$"${external.idp.token-uri}")
	private val idpTokenUri: String,
	@param:Value($$"${registry.security.rate-limit.auth.capacity}")
	private val authRateLimitCapacity: Int,
	@param:Value($$"${registry.security.rate-limit.auth.window-seconds}")
	private val authRateLimitWindowSeconds: Long,
	@param:Value($$"${registry.security.rate-limit.auth.max-tracked-clients}")
	private val authRateLimitMaxTrackedClients: Long,
) {

	@Bean
	@Order(1)
	fun documentationSecurityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
		return http
			.securityMatcher(documentationMatcher())
			.authorizeExchange { it.anyExchange().permitAll() }
			.configureSecurityHeaders(null)
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

	@Bean
	@Order(2)
	fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
		return http
			.resolveLocaleContext()
			.configureSecurityHeaders(API_CONTENT_SECURITY_POLICY)
			.rateLimitAuthentication()
			.exposeCsrfToken()
			.configureCsrf()
			.configureResourceAccess()
			.disableAuthForm()
			.disableDefaultLogout()
			.configureOAuth2Server()
			.handleException()
			.build()
	}

	private fun ServerHttpSecurity.resolveLocaleContext() = addFilterBefore(localeContextHandler, FIRST)

	private fun ServerHttpSecurity.configureSecurityHeaders(contentSecurityPolicy: String?) = headers { headerSpec ->
		if (contentSecurityPolicy != null) headerSpec.contentSecurityPolicy { it.policyDirectives(contentSecurityPolicy) }
		headerSpec.permissionsPolicy { it.policy(PERMISSIONS_POLICY) }
		headerSpec.hsts {
			it.maxAge(Duration.ofDays(365))
			it.includeSubdomains(true)
			it.preload(true)
		}
	}

	private fun ServerHttpSecurity.rateLimitAuthentication() = addFilterBefore(
		AuthenticationRateLimitHandler(
			translateService,
			localeContextResolver,
			objectMapper,
			clock,
			authRateLimitCapacity,
			authRateLimitWindowSeconds,
			authRateLimitMaxTrackedClients,
		),
		FIRST,
	)

	private fun ServerHttpSecurity.exposeCsrfToken() = addFilterBefore(CsrfTokenHeaderHandler(csrfTokenService), FIRST)

	private fun ServerHttpSecurity.configureCsrf() = csrf { csrfSpec ->
		csrfSpec.csrfTokenRepository(csrfTokenService)
		csrfSpec.requireCsrfProtectionMatcher(csrfRequiredMatcher())
		csrfSpec.accessDeniedHandler(authorizationErrorHandler.accessDeniedHandler())
		csrfSpec.csrfTokenRequestHandler(ServerCsrfTokenRequestAttributeHandler())
	}

	private fun csrfRequiredMatcher() = ServerWebExchangeMatcher { exchange ->
		val request = exchange.request
		val isSafeMethod = request.method in CSRF_SAFE_METHODS
		val hasBearerAuth = request.headers.getFirst(AUTHORIZATION)?.startsWith("Bearer ") == true
		val isExemptPath = CSRF_EXEMPT_PATHS.any { pathMatcher.match(it, request.path.value()) }
		if (!isSafeMethod && !hasBearerAuth && !isExemptPath) MatchResult.match() else MatchResult.notMatch()
	}

	internal companion object {
		val pathMatcher = AntPathMatcher()
		val CSRF_SAFE_METHODS = setOf(GET, HEAD, OPTIONS, TRACE)

		val CSRF_EXEMPT_PATHS = listOf("/api/*/authentication/token", "/api/*/authentication/token/refresh")

		val DOCUMENTATION_PATHS = listOf("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")

		private val HOST_NAME_PATTERN = Regex("[A-Za-z0-9.\\-\\[\\]:]+")

		internal fun apiOrigin(scheme: String, hostName: String, apiPort: Int): String? {
			return if (HOST_NAME_PATTERN.matches(hostName)) "$scheme://$hostName:$apiPort" else null
		}

		internal fun documentationContentSecurityPolicy(idpOrigin: String, apiOrigin: String?): String {
			val connectSources = listOfNotNull("'self'", idpOrigin, apiOrigin).joinToString(" ")
			return "default-src 'self'; connect-src $connectSources; frame-ancestors 'none'"
		}

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

		const val API_CONTENT_SECURITY_POLICY = "default-src 'none'; frame-ancestors 'none'"

		const val PERMISSIONS_POLICY =
			"geolocation=(), camera=(), microphone=(), payment=(), usb=(), interest-cohort=()"
	}

	private fun ServerHttpSecurity.configureResourceAccess() = authorizeExchange {
		it.matchers(managementPortMatcher()).permitAll()
		it.pathMatchers(GET, "/api/*/authentication/login/uri", "/api/*/authentication/logout/uri").permitAll()
		it.pathMatchers(POST, "/api/*/authentication/token", "/api/*/authentication/token/refresh").permitAll()
		// Libellés statiques non sensibles (enums) : publics pour éviter la résolution de l'authentification et des autorités à chaque appel
		it.pathMatchers(GET, "/api/*/metadata/**").permitAll()
		it.anyExchange().authenticated()
	}

	private fun managementPortMatcher() = ServerWebExchangeMatcher { exchange ->
		if (exchange.request.localAddress?.port == managementPort) MatchResult.match() else MatchResult.notMatch()
	}

	private fun ServerHttpSecurity.disableAuthForm() = formLogin { it.disable() }

	private fun ServerHttpSecurity.disableDefaultLogout() = logout { it.disable() }

	private fun ServerHttpSecurity.configureOAuth2Server() = oauth2ResourceServer { resourceServer ->
		resourceServer.authenticationFailureHandler(authorizationErrorHandler)
		resourceServer.bearerTokenConverter(cookieBearerTokenHandler)
		resourceServer.jwt {
			it.jwtAuthenticationConverter(tokenConverter)
		}
	}

	private fun ServerHttpSecurity.handleException() = exceptionHandling {
		it.accessDeniedHandler(authorizationErrorHandler.accessDeniedHandler())
		it.authenticationEntryPoint(authorizationErrorHandler.unauthorizedHandler())
	}

	@Bean
	fun expressionHandler(): MethodSecurityExpressionHandler {
		val handler = DefaultMethodSecurityExpressionHandler()
		handler.setPermissionEvaluator(PermissionService())
		return handler
	}

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
}
