package fr.laucoin.registry.backend.config

import fr.laucoin.registry.backend.domain.handler.AuthorizationErrorHandler
import fr.laucoin.registry.backend.domain.handler.HeadersHandler
import fr.laucoin.registry.backend.domain.service.impl.PermissionService
import fr.laucoin.registry.backend.domain.service.impl.TokenConverterService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.POST
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
    @Value("\${external.frontend.base-url}")
    private val frontendUrl: String,
    @Value("\${registry.feature.documentation.enabled:false}")
    private val documentationEnabled: Boolean,
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
            it.pathMatchers(GET, "/", "/swagger-ui.html", "/api-docs/**", "/webjars/swagger-ui/**").permitAll()
        }
        it.pathMatchers(GET, "/api/authentication/login/uri", "/api/authentication/logout/uri").permitAll()
        it.pathMatchers(POST, "/api/authentication/token", "/api/authentication/token/refresh").permitAll()
        it.anyExchange().authenticated()
    }

    private fun ServerHttpSecurity.disableAuthForm() = formLogin { it.disable() }

    private fun ServerHttpSecurity.configureLogout() = logout {
        val logoutUrl = "/logout"
        it.logoutUrl(logoutUrl)
        it.requiresLogout(ServerWebExchangeMatchers.pathMatchers(GET, *arrayOf(logoutUrl)))
    }

    private fun ServerHttpSecurity.configureOAuth2Server() = oauth2ResourceServer { ressourceServer ->
        ressourceServer.jwt { it.jwtAuthenticationConverter(tokenConverter) }
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
        configuration.allowedOrigins = listOf(frontendUrl)
        configuration.allowedMethods = listOf("HEAD", "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        configuration.allowCredentials = true
        configuration.allowedHeaders = listOf(
            "Authorization",
            "Cache-Control",
            "Content-Type",
            "Access-Control-Allow-Origin",
            "Access-Control-Expose-Headers",
            "Access-Control-Allow-Headers"
        )
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
