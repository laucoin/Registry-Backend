package fr.laucoin.registry.backend.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.Scopes
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.security.SecurityScheme.Type.OAUTH2
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*


@Configuration
@ConditionalOnProperty(value = ["registry.feature.documentation.enabled"], havingValue = "true", matchIfMissing = false)
@OpenAPIDefinition(
    info = Info(
        title = $$"${registry.information.name}",
        description = $$"${registry.information.description}",
        contact = Contact(
            name = $$"${registry.information.support.name}",
            email = $$"${registry.information.support.email}",
            url = $$"${registry.information.support.url}"
        )
    )
)
class SwaggerConfig(
    @param:Value($$"${registry.security.oauth2.authorization-uri:}")
    private val configAuthUrl: String?,
    @param:Value($$"${registry.security.oauth2.token-uri:}")
    private val configTokenUrl: String?,
    @param:Value($$"${registry.server.prefix:''}")
    private val configApiPrefix: String,
) {
    private companion object {
        private const val CLIENT_NAME = "OAuth2"

        /**
         * `email` is not optional: TokenConverterService refuses a token without it, so a Swagger
         * session opened without this scope authenticates against the provider and is then rejected
         * here — which reads as a broken API rather than a missing scope.
         *
         * `offline_access` is deliberately absent. It would have the provider mint a refresh token
         * into the browser, and Swagger has no use for one.
         */
        private val SCOPES = Scopes()
            .addString("openid", "Required by OpenID Connect")
            .addString("profile", "First and last names, for the auto-provisioned user")
            .addString("email", "Email address, which Registry uses to identify the account")
    }

    @Bean
    fun openApi(): OpenAPI {
        val openAPI = OpenAPI()
        if (Objects.nonNull(configAuthUrl) && Objects.nonNull(configTokenUrl)) {
            openAPI.components(
                Components()
                    .addSecuritySchemes(
                        CLIENT_NAME,
                        SecurityScheme()
                            .type(OAUTH2)
                            // Authorization code with PKCE, not implicit. The implicit flow returns
                            // the token in the URL fragment, where it lands in browser history and in
                            // any Referer that leaks — OAuth 2.0 Security Best Current Practice has
                            // dropped it for that reason. PKCE is what makes the exchange safe for a
                            // browser client that holds no secret; springdoc enables it through
                            // `springdoc.swagger-ui.use-pkce-with-authorization-code-grant`.
                            .flows(
                                OAuthFlows()
                                    .authorizationCode(
                                        OAuthFlow().apply {
                                            authorizationUrl = configAuthUrl
                                            refreshUrl = configTokenUrl
                                            tokenUrl = configTokenUrl
                                            scopes = SCOPES
                                        }
                                    )
                            )
                    )
            ).security(listOf(SecurityRequirement().addList(CLIENT_NAME)))
        }
        return openAPI
    }

    @Bean
    fun customOpenApi(): OpenApiCustomizer {
        return OpenApiCustomizer { openApi: OpenAPI ->
            openApi.paths.entries.removeIf { entry: Map.Entry<String, PathItem?> ->
                !entry.key.startsWith(configApiPrefix)
            }
        }
    }

    @Bean
    fun securitiesApis(): GroupedOpenApi {
        return GroupedOpenApi.builder().group("security")
            .pathsToMatch("/api/v*/authentication/**").build()
    }

    @Bean
    fun metadataApis(): GroupedOpenApi {
        return GroupedOpenApi.builder().group("metadata")
            .pathsToMatch("/api/v*/metadata/**").build()
    }

    @Bean
    fun usersApis(): GroupedOpenApi {
        return GroupedOpenApi.builder().group("users")
            .pathsToMatch("/api/v*/users/**").build()
    }

    @Bean
    fun projectsApis(): GroupedOpenApi {
        return GroupedOpenApi.builder().group("projects")
            .pathsToMatch(
                "/api/v*/projects",
                "/api/v*/projects/{id}",
                "/api/v*/projects/{id}/disable",
                "/api/v*/projects/{id}/enable",
            ).build()
    }

    @Bean
    fun projectProfilesApis(): GroupedOpenApi {
        return GroupedOpenApi.builder().group("project-profiles")
            .pathsToMatch("/api/v*/projects/{projectId}/profiles/**").build()
    }

    @Bean
    fun projectParticipantsApis(): GroupedOpenApi {
        return GroupedOpenApi.builder().group("project-participants")
            .pathsToMatch(
                "/api/v*/projects/{projectId}/participants/**",
                "/api/v*/projects/{projectId}/groups/**",
            ).build()
    }

    @Bean
    fun projectMovementsApis(): GroupedOpenApi {
        return GroupedOpenApi.builder().group("project-movements")
            .pathsToMatch("/api/v*/projects/{projectId}/movements/**").build()
    }

    @Bean
    fun projectVehiclesApis(): GroupedOpenApi {
        return GroupedOpenApi.builder().group("project-vehicles")
            .pathsToMatch("/api/v*/projects/{projectId}/vehicles/**").build()
    }

    @Bean
    fun projectActivitiesApis(): GroupedOpenApi {
        return GroupedOpenApi.builder().group("project-activities")
            .pathsToMatch("/api/v*/projects/{projectId}/activities/**").build()
    }

    @Bean
    fun projectCommunicationsApis(): GroupedOpenApi {
        return GroupedOpenApi.builder().group("project-communications")
            .pathsToMatch("/api/v*/projects/{projectId}/communications/**").build()
    }

    @Bean
    fun projectAlertsApis(): GroupedOpenApi {
        return GroupedOpenApi.builder().group("project-alerts")
            .pathsToMatch("/api/v*/projects/{projectId}/alerts/**").build()
    }

    @Bean
    fun purgesApis(): GroupedOpenApi {
        return GroupedOpenApi.builder().group("data-purges")
            .pathsToMatch("/api/v*/purge/**").build()
    }
}
