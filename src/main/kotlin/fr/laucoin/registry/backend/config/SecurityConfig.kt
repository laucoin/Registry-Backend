package fr.laucoin.registry.backend.config

import fr.laucoin.registry.backend.domain.handler.AuthorizationErrorHandler
import fr.laucoin.registry.backend.domain.handler.CsrfTokenHandler
import fr.laucoin.registry.backend.domain.handler.DocumentationRedirectHandler
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
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN
import org.springframework.security.web.server.header.ServerHttpHeadersWriter
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter.Mode.DENY
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono


@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig(
	private val tokenConverter: TokenConverterService,
	private val authorizationErrorHandler: AuthorizationErrorHandler,
	private val headersHandler: HeadersHandler,
	private val csrfTokenHandler: CsrfTokenHandler,
	private val tokenExtractionHandler: TokenExtractionHandler,
	private val documentationRedirectHandler: DocumentationRedirectHandler,
	@param:Value($$"${registry.security.cookie.domain:}")
	private val cookieDomain: String,
	@param:Value($$"${registry.security.cookie.secure:true}")
	private val cookieSecure: Boolean,
	@param:Value($$"${registry.security.cookie.same-site:Lax}")
	private val cookieSameSite: String,
	@param:Value($$"${external.cors.urls}")
	private val corsUrls: List<String>,
	@param:Value($$"${management.server.port}")
	private val managementPort: Int,
	@param:Value($$"${registry.feature.documentation.enabled:false}")
	private val documentationEnabled: Boolean,
	@param:Value($$"${registry.feature.observability.enabled:false}")
	private val observabilityEnabled: Boolean,
) {
	private companion object {
		private val SESSION_OPENING_PATH = Regex("^/api/v\\d+/authentication/token$")
		private const val CSRF_HEADER = "X-XSRF-TOKEN"
		private const val CSP_HEADER = "Content-Security-Policy"
		private const val API_CSP = "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"
		private const val DOCUMENTATION_CSP = "frame-ancestors 'none'"
		private const val PERMISSIONS_POLICY = "camera=(), microphone=(), geolocation=(), payment=()"
	}

	@Bean
	fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
		return http
			.configureCsrf()
			.securityHeaders()
			.addDocumentationRedirect()
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
		it.csrfTokenRequestHandler(ServerCsrfTokenRequestAttributeHandler())
	}

	private fun csrfTokenRepository() = CookieServerCsrfTokenRepository.withHttpOnlyFalse().apply {
		setCookieCustomizer {
			it.secure(cookieSecure).sameSite(cookieSameSite)
			if (cookieDomain.isNotBlank()) it.domain(cookieDomain)
		}
	}

	private fun csrfProtectionMatcher(): ServerWebExchangeMatcher {
		val stateChanging = CsrfWebFilter.DEFAULT_CSRF_MATCHER
		return ServerWebExchangeMatcher { exchange ->
			val request = exchange.request
			val exempt = request.headers.getFirst(AUTHORIZATION) != null
					|| (request.method == POST && SESSION_OPENING_PATH.matches(
				request.path.pathWithinApplication().value()
			))
			if (exempt) MatchResult.notMatch() else stateChanging.matches(exchange)
		}
	}

	private fun ServerHttpSecurity.securityHeaders() = headers {
		it.referrerPolicy { policy -> policy.policy(STRICT_ORIGIN_WHEN_CROSS_ORIGIN) }
		it.permissionsPolicy { policy -> policy.policy(PERMISSIONS_POLICY) }
		it.frameOptions { frame -> frame.mode(DENY) }
		it.writer(contentSecurityPolicyWriter())
	}

	fun contentSecurityPolicyWriter() = ServerHttpHeadersWriter { exchange ->
		exchange.response.headers
			.set(CSP_HEADER, if (exchange.isOnManagementPort()) DOCUMENTATION_CSP else API_CSP)
		Mono.empty()
	}

	private fun ServerWebExchange.isOnManagementPort() = request.localAddress?.port == managementPort

	fun onManagementPort() = ServerWebExchangeMatcher { exchange ->
		if (exchange.isOnManagementPort()) MatchResult.match() else MatchResult.notMatch()
	}

	private fun ServerHttpSecurity.addLocaleFilter() = addFilterBefore(headersHandler, FIRST)

	private fun ServerHttpSecurity.addDocumentationRedirect() = addFilterBefore(documentationRedirectHandler, FIRST)

	private fun ServerHttpSecurity.configureResourceAccess() = authorizeExchange {
		if (observabilityEnabled || documentationEnabled) {
			it.matchers(onManagementPort()).permitAll()
		}
		it.pathMatchers(GET, "/api/*/authentication/login/uri", "/api/*/authentication/logout/uri").permitAll()
		it.pathMatchers(POST, "/api/*/authentication/token", "/api/*/authentication/token/refresh").permitAll()
		it.anyExchange().authenticated()
	}

	private fun ServerHttpSecurity.disableAuthForm() = formLogin { it.disable() }

	private fun ServerHttpSecurity.configureOAuth2Server() = oauth2ResourceServer { resourceServer ->
		resourceServer.authenticationFailureHandler(authorizationErrorHandler)
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
		configuration.allowCredentials = true
		configuration.allowedHeaders = listOf(
			AUTHORIZATION,
			CACHE_CONTROL,
			CONTENT_TYPE,
			ACCEPT_LANGUAGE,
			CSRF_HEADER,
		)
		val source = UrlBasedCorsConfigurationSource()
		source.registerCorsConfiguration("/**", configuration)
		return source
	}
}
