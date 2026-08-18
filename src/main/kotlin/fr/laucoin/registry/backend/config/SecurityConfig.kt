package fr.laucoin.registry.backend.config

import fr.laucoin.registry.backend.domain.handler.AuthorizationErrorHandler
import fr.laucoin.registry.backend.domain.constant.AuditConst.CORRELATION_ID_HEADER
import fr.laucoin.registry.backend.domain.handler.HeadersHandler
import fr.laucoin.registry.backend.domain.service.impl.PermissionService
import fr.laucoin.registry.backend.domain.service.impl.TokenConverterService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.CACHE_CONTROL
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpHeaders.ETAG
import org.springframework.http.HttpHeaders.IF_NONE_MATCH
import org.springframework.http.HttpHeaders.RETRY_AFTER
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
import org.springframework.security.config.web.server.SecurityWebFiltersOrder.FIRST
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
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
	@param:Value($$"${external.cors.urls}")
	private val corsUrls: List<String>,
	@param:Value($$"${registry.feature.documentation.enabled:false}")
	private val documentationEnabled: Boolean,
	@param:Value($$"${registry.feature.observability.enabled:false}")
	private val observabilityEnabled: Boolean,
) {

	@Bean
	fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
		return http
			.disableCsrf()
			.handleHeaders()
			.configureResourceAccess()
			.disableAuthForm()
			.configureLogout()
			.configureOAuth2Server()
			.handleException()
			.build()
	}

	private fun ServerHttpSecurity.disableCsrf() = csrf { it.disable() }

	private fun ServerHttpSecurity.handleHeaders() = addFilterBefore(headersHandler, FIRST)

	private fun ServerHttpSecurity.configureResourceAccess() = authorizeExchange {
		if (documentationEnabled) {
			it.pathMatchers(GET, "/", "/swagger-ui.html", "/api-docs/**", "/webjars/swagger-ui/**", "/swagger-ui/**")
				.permitAll()
		}
		if (observabilityEnabled) {
			it.pathMatchers(GET, "/actuator/**").permitAll()
		}
		it.pathMatchers(GET, "/api/v1/authentication/login/uri", "/api/v1/authentication/logout/uri").permitAll()
		it.pathMatchers(POST, "/api/v1/authentication/token", "/api/v1/authentication/token/refresh").permitAll()
		it.anyExchange().authenticated()
	}

	private fun ServerHttpSecurity.disableAuthForm() = formLogin { it.disable() }

	private fun ServerHttpSecurity.configureLogout() = logout {
		val logoutUrl = "/logout"
		it.logoutUrl(logoutUrl)
		it.requiresLogout(ServerWebExchangeMatchers.pathMatchers(GET, *arrayOf(logoutUrl)))
	}

	private fun ServerHttpSecurity.configureOAuth2Server() = oauth2ResourceServer { resourceServer ->
		resourceServer.authenticationFailureHandler(authorizationErrorHandler)
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
			IF_NONE_MATCH,
			CORRELATION_ID_HEADER,
		)
		configuration.exposedHeaders = listOf(
			ETAG,
			RETRY_AFTER,
			CORRELATION_ID_HEADER,
		)
		val source = UrlBasedCorsConfigurationSource()
		source.registerCorsConfiguration("/**", configuration)
		return source
	}
}
