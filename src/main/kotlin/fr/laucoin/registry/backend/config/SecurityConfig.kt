package fr.laucoin.registry.backend.config

import fr.laucoin.registry.backend.domain.handler.AuthorizationErrorHandler
import fr.laucoin.registry.backend.domain.handler.CsrfTokenHandler
import fr.laucoin.registry.backend.domain.handler.HeadersHandler
import fr.laucoin.registry.backend.domain.handler.TokenExtractionHandler
import fr.laucoin.registry.backend.domain.service.impl.PermissionService
import fr.laucoin.registry.backend.domain.service.impl.TokenConverterService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.CACHE_CONTROL
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpMethod.DELETE
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.HEAD
import org.springframework.http.HttpMethod.OPTIONS
import org.springframework.http.HttpMethod.PATCH
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpMethod.PUT
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder.CSRF
import org.springframework.security.config.web.server.SecurityWebFiltersOrder.FIRST
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository
import org.springframework.security.web.server.csrf.CsrfWebFilter
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource


@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig(
	private val tokenConverter: TokenConverterService,
	private val authorizationErrorHandler: AuthorizationErrorHandler,
	private val headersHandler: HeadersHandler,
	private val csrfTokenHandler: CsrfTokenHandler,
	private val tokenExtractionHandler: TokenExtractionHandler,
	@param:Value($$"${registry.security.cookie.domain:}")
	private val cookieDomain: String,
	@param:Value($$"${registry.security.cookie.secure:true}")
	private val cookieSecure: Boolean,
	@param:Value($$"${registry.security.cookie.same-site:Lax}")
	private val cookieSameSite: String,
	@param:Value($$"${external.cors.urls}")
	private val corsUrls: List<String>,
	@param:Value($$"${registry.feature.documentation.enabled:false}")
	private val documentationEnabled: Boolean,
	@param:Value($$"${registry.feature.observability.enabled:false}")
	private val observabilityEnabled: Boolean,
) {
	private companion object {
		/** Matches `/api/<version>/authentication/token` exactly — never `/token/refresh`. */
		private val SESSION_OPENING_PATH = Regex("^/api/v\\d+/authentication/token$")

		/** Where the double-submit token comes back; the name Spring's cookie repository expects. */
		private const val CSRF_HEADER = "X-XSRF-TOKEN"
	}

	@Bean
	fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
		return http
			.configureCsrf()
			.addLocaleFilter()
			.addFilterAt(csrfTokenHandler, CSRF)
			.configureResourceAccess()
			.disableAuthForm()
			.configureOAuth2Server()
			.handleException()
			.build()
	}

	private fun ServerHttpSecurity.configureCsrf() = csrf {
		it.csrfTokenRepository(csrfTokenRepository())
		it.requireCsrfProtectionMatcher(csrfProtectionMatcher())
		// Spring defaults to the XOR handler, which masks the token to blunt BREACH. That assumes the
		// token is rendered into a response body; ours travels in a cookie the frontend reads and
		// echoes back verbatim, so the masked value never matches the raw one and every mutating call
		// is refused. BREACH is not a concern here precisely because the token is never in a body.
		it.csrfTokenRequestHandler(ServerCsrfTokenRequestAttributeHandler())
	}

	/**
	 * The token is readable by script on purpose — that is the double-submit pattern: the browser
	 * returns it in a header, and only same-origin script can read the cookie to do so. It is
	 * scoped like the session cookies so the frontend, served from a sibling host, can read it.
	 */
	private fun csrfTokenRepository() = CookieServerCsrfTokenRepository.withHttpOnlyFalse().apply {
		setCookieCustomizer {
			it.secure(cookieSecure).sameSite(cookieSameSite)
			if (cookieDomain.isNotBlank()) it.domain(cookieDomain)
		}
	}

	/**
	 * CSRF applies to state-changing requests, minus two deliberate exemptions.
	 *
	 * A request carrying an `Authorization` header authenticates through a credential the caller set
	 * itself — Swagger, a service account, any non-browser client — and a browser never attaches that
	 * header to a cross-site request, so such a caller cannot be a CSRF victim. This exemption is only
	 * sound because [TokenExtractionHandler] reads the header **before** the cookie: were the cookie to
	 * win, a request could carry a meaningless header to claim the exemption while authenticating
	 * through the ambient cookie.
	 *
	 * `POST /authentication/token` opens the session, so no ambient credential exists yet and there is
	 * nothing to protect. `/token/refresh` is deliberately **not** exempt — that one runs entirely on
	 * the refresh cookie, which is exactly the shape CSRF attacks.
	 */
	private fun csrfProtectionMatcher(): ServerWebExchangeMatcher {
		val stateChanging = CsrfWebFilter.DEFAULT_CSRF_MATCHER
		return ServerWebExchangeMatcher { exchange ->
			val request = exchange.request
			val exempt = request.headers.getFirst(AUTHORIZATION) != null
				|| (request.method == POST && SESSION_OPENING_PATH.matches(request.path.pathWithinApplication().value()))
			if (exempt) MatchResult.notMatch() else stateChanging.matches(exchange)
		}
	}

	/** Named for what it does: [HeadersHandler] resolves the request locale, it sets no headers. */
	private fun ServerHttpSecurity.addLocaleFilter() = addFilterBefore(headersHandler, FIRST)

	private fun ServerHttpSecurity.configureResourceAccess() = authorizeExchange {
		// Health, metrics and the API documentation are all served from the management port, under
		// `/actuator`. This chain governs that port too — with the rule removed they answer 401, which
		// is what a probe would then report as an outage — so the two features share one matcher.
		//
		// Left open because a liveness probe and a Prometheus scraper have no credentials to present.
		// That is only safe as long as the management port stays off the public ingress, which is the
		// whole reason for separating it; see the README.
		if (observabilityEnabled || documentationEnabled) {
			it.pathMatchers(GET, "/actuator/**").permitAll()
		}
		it.pathMatchers(GET, "/api/*/authentication/login/uri", "/api/*/authentication/logout/uri").permitAll()
		it.pathMatchers(POST, "/api/*/authentication/token", "/api/*/authentication/token/refresh").permitAll()
		it.anyExchange().authenticated()
	}

	private fun ServerHttpSecurity.disableAuthForm() = formLogin { it.disable() }

	private fun ServerHttpSecurity.configureOAuth2Server() = oauth2ResourceServer { resourceServer ->
		resourceServer.authenticationFailureHandler(authorizationErrorHandler)
		// Reads the token from the Authorization header, then from the session cookie.
		resourceServer.bearerTokenConverter(tokenExtractionHandler)
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
		configuration.allowedMethods = listOf(
			HEAD.name(),
			GET.name(),
			POST.name(),
			PUT.name(),
			DELETE.name(),
			PATCH.name(),
			OPTIONS.name(),
		)
		// Required now that the session travels in cookies: without it the browser sends none of them
		// cross-origin, and every authenticated call from the SPA fails. It also forbids a wildcard
		// origin, which is a useful guard on `external.cors.urls`.
		configuration.allowCredentials = true
		configuration.allowedHeaders = listOf(
			AUTHORIZATION,
			CACHE_CONTROL,
			CONTENT_TYPE,
			ACCEPT_LANGUAGE,
			// The CSRF token the SPA reads from its cookie and echoes back. Angular's built-in XSRF
			// support does not apply — it only attaches the header to same-origin requests, and the
			// SPA sits on a sibling host — so the interceptor sets it itself.
			CSRF_HEADER,
		)
		val source = UrlBasedCorsConfigurationSource()
		source.registerCorsConfiguration("/**", configuration)
		return source
	}
}
