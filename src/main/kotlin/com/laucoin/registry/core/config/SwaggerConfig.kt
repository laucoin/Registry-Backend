package com.laucoin.registry.core.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.Scopes
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.security.SecurityScheme.Type.OAUTH2
import java.util.Objects
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
@OpenAPIDefinition(
    info = Info(
        title = "\${registry.information.name}",
        description = "\${registry.information.description}"
    )
)
class SwaggerConfig(
    @Value("\${registry.security.sso.auth-url:}") private val keycloakAuthUrl: String?,
    @Value("\${registry.security.sso.token-url:}") private val keycloakTokenUrl: String?,
) {
    private val securityRequirementName: String = "SGDF Keycloak"

    @Bean
    fun openApi(): OpenAPI {
        val openAPI = OpenAPI()
        if (Objects.nonNull(keycloakAuthUrl) && Objects.nonNull(keycloakTokenUrl)) {
            openAPI.components(
                Components()
                    .addSecuritySchemes(
                        securityRequirementName,
                        SecurityScheme()
                            .type(OAUTH2)
                            .flows(
                                OAuthFlows()
                                    .implicit(
                                        OAuthFlow().apply {
                                            authorizationUrl = keycloakAuthUrl
                                            refreshUrl = keycloakTokenUrl
                                            tokenUrl = keycloakTokenUrl
                                            scopes = Scopes()
                                        }
                                    )
                            )
                    )
            ).security(listOf(SecurityRequirement().addList(securityRequirementName)))
        }
        return openAPI
    }
}
