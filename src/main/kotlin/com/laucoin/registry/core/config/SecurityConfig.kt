package com.laucoin.registry.core.config

import com.laucoin.registry.core.adapter.KeycloakAdapter
import com.laucoin.registry.core.adapter.RegistryPermissionEvaluator
import com.laucoin.registry.core.adapter.SecurityProperties
import com.laucoin.registry.core.service.IAuthService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS
import org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN
import org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpMethod.DELETE
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.OPTIONS
import org.springframework.http.HttpMethod.PATCH
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpMethod.PUT
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig(
    @Value("\${external.frontend.base-url}")
    private val frontendBaseUrl: String,
    @Value("\${registry.feature.swagger.enabled:false}")
    private val enableSwagger: Boolean,
    private val jwtDecoder: ReactiveJwtDecoder,
    private val service: IAuthService,
    private val securityProperties: SecurityProperties,
) {
    @Bean
    fun securityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange {
                if (enableSwagger) {
                    it.pathMatchers("/api-docs/**", "/swagger-ui.html", "/webjars/swagger-ui/**").permitAll()
                }

                it.anyExchange().authenticated()
            }
            .oauth2ResourceServer {
                it.jwt { jwtConfigurer ->
                    jwtConfigurer
                        .authenticationManager(KeycloakAdapter(jwtDecoder, service, securityProperties))
                }
            }
            .build()
    }

    @Bean
    fun expressionHandler(): MethodSecurityExpressionHandler {
        val handler = DefaultMethodSecurityExpressionHandler()
        handler.setPermissionEvaluator(RegistryPermissionEvaluator())
        return handler
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        return UrlBasedCorsConfigurationSource()
            .apply {
                registerCorsConfiguration(
                    "/**",
                    CorsConfiguration().apply {
                        allowedOrigins = listOf(frontendBaseUrl)
                        allowedMethods = listOf(GET.name(), POST.name(), PUT.name(), PATCH.name(), DELETE.name(), OPTIONS.name())
                        allowCredentials = true

                        allowedHeaders = listOf(
                            ACCESS_CONTROL_ALLOW_ORIGIN,
                            ACCESS_CONTROL_ALLOW_HEADERS,
                            ACCESS_CONTROL_EXPOSE_HEADERS,
                            AUTHORIZATION,
                        )
                    }
                )
            }
    }
}
