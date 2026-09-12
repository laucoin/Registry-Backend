package fr.laucoin.registry.backend.config

import com.nimbusds.jose.shaded.gson.Gson
import fr.laucoin.registry.backend.domain.handler.AuthenticationRateLimitHandler
import fr.laucoin.registry.backend.domain.handler.AuthorizationErrorHandler
import fr.laucoin.registry.backend.domain.handler.CookieBearerTokenHandler
import fr.laucoin.registry.backend.domain.handler.CsrfTokenHeaderHandler
import fr.laucoin.registry.backend.domain.handler.HeadersHandler
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.domain.service.impl.CsrfTokenService
import fr.laucoin.registry.backend.domain.service.impl.CsrfTokenService.Companion.HEADER_NAME
import fr.laucoin.registry.backend.domain.service.impl.PermissionService
import fr.laucoin.registry.backend.domain.service.impl.TokenConverterService
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
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult
import org.springframework.util.AntPathMatcher
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
	private val cookieBearerTokenHandler: CookieBearerTokenHandler,
	private val translateService: ITranslateService,
	private val gson: Gson,
	private val csrfTokenService: CsrfTokenService,
	@param:Value($$"${external.cors.urls}")
	private val corsUrls: List<String>,
	@param:Value($$"${registry.server.management-port}")
	private val managementPort: Int,
	@param:Value($$"${registry.security.rate-limit.auth.capacity}")
	private val authRateLimitCapacity: Int,
	@param:Value($$"${registry.security.rate-limit.auth.window-seconds}")
	private val authRateLimitWindowSeconds: Long,
) {

	@Bean
	fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
		return http
			.handleHeaders()
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

	private fun ServerHttpSecurity.handleHeaders() = addFilterBefore(headersHandler, FIRST)

	private fun ServerHttpSecurity.rateLimitAuthentication() = addFilterBefore(
		AuthenticationRateLimitHandler(translateService, gson, authRateLimitCapacity, authRateLimitWindowSeconds),
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
		val isExemptPath = CSRF_EXEMPT_PATHS.any { csrfPathMatcher.match(it, request.path.value()) }
		if (!isSafeMethod && !hasBearerAuth && !isExemptPath) MatchResult.match() else MatchResult.notMatch()
	}

	private companion object {
		val csrfPathMatcher = AntPathMatcher()
		val CSRF_SAFE_METHODS = setOf(GET, HEAD, OPTIONS, TRACE)

		val CSRF_EXEMPT_PATHS = listOf("/api/*/authentication/token", "/api/*/authentication/token/refresh")
	}

	private fun ServerHttpSecurity.configureResourceAccess() = authorizeExchange {
		it.matchers(managementPortMatcher()).permitAll()
		it.pathMatchers(GET, "/api/*/authentication/login/uri", "/api/*/authentication/logout/uri").permitAll()
		it.pathMatchers(POST, "/api/*/authentication/token", "/api/*/authentication/token/refresh").permitAll()
		it.anyExchange().authenticated()
	}

	private fun managementPortMatcher() = ServerWebExchangeMatcher { exchange ->
		if (exchange.request.localAddress?.port == managementPort) MatchResult.match() else MatchResult.notMatch()
	}

	private fun ServerHttpSecurity.disableAuthForm() = formLogin { it.disable() }

	// Leaving logout {} unconfigured wouldn't remove it — Spring still applies its own default
	// (a GET /logout endpoint) unless explicitly disabled. The app's real logout flow clears the
	// auth cookies and revokes the tokens through /api/v1/authentication/logout/uri instead; this
	// default has no notion of that and was never wired into the frontend.
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
			ACCESS_CONTROL_ALLOW_ORIGIN,
			ACCESS_CONTROL_ALLOW_HEADERS,
			ACCESS_CONTROL_EXPOSE_HEADERS,
			HEADER_NAME,
		)
		configuration.exposedHeaders = listOf(HEADER_NAME)
		val source = UrlBasedCorsConfigurationSource()
		source.registerCorsConfiguration("/**", configuration)
		return source
	}
}
