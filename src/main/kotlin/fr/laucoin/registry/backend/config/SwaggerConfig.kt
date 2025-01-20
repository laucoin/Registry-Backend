package fr.laucoin.registry.backend.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.security.SecurityScheme.Type.OAUTH2
import java.util.Objects
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
@ConditionalOnProperty(value = ["registry.feature.documentation.enabled"], havingValue = "true", matchIfMissing = false)
@OpenAPIDefinition(
    info = Info(
        title = "\${registry.information.name}",
        description = "\${registry.information.description}",
        contact = Contact(
            name = "\${registry.information.support.name}",
            email = "\${registry.information.support.email}",
            url = "\${registry.information.support.url}"
        )
    )
)
class SwaggerConfig(
    @Value("\${registry.security.oauth2.url:}/auth")
    private val authUrl: String?,
    @Value("\${registry.security.oauth2.url:}/token")
    private val tokenUrl: String?,
    @Value("\${registry.server.prefix:''}")
    private val apiPrefix: String,
) {
    companion object {
        private const val CLIENT_NAME = "OAuth2"
    }

    @Bean
    fun openApi(): OpenAPI {
        val openAPI = OpenAPI()
        if (Objects.nonNull(authUrl) && Objects.nonNull(tokenUrl)) {
            openAPI.components(
                Components()
                    .addSecuritySchemes(
                        CLIENT_NAME,
                        SecurityScheme()
                            .type(OAUTH2)
                            .flows(
                                OAuthFlows()
                                    .implicit(
                                        OAuthFlow().apply {
                                            authorizationUrl = authUrl
                                            refreshUrl = tokenUrl
                                            tokenUrl = tokenUrl
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
            openApi.paths.entries.removeIf { entry: Map.Entry<String, PathItem?> -> ! entry.key.startsWith(apiPrefix) }
        }
    }

    @Bean
    fun securitiesApis(): GroupedOpenApi {
        return GroupedOpenApi.builder().group("Security Management")
            .pathsToMatch("/api/authentication/**").build()
    }

    @Bean
    fun usersApis(): GroupedOpenApi {
        return GroupedOpenApi.builder().group("Users Management")
            .pathsToMatch("/api/users/**").build()
    }

    @Bean
    fun eventsApis(): GroupedOpenApi {
        return GroupedOpenApi.builder().group("Events Management")
            .pathsToMatch(
                "/api/events",
                "/api/events/{id}",
                "/api/events/{id}/disable",
                "/api/events/{id}/enable",
            ).build()
    }

    @Bean
    fun eventProfilesApis(): GroupedOpenApi {
        return GroupedOpenApi.builder().group("Event's Profiles Management")
            .pathsToMatch("/api/events/{eventId}/profiles/**").build()
    }

    @Bean
    fun eventParticipantsApis(): GroupedOpenApi {
        return GroupedOpenApi.builder().group("Event's Participants Management")
            .pathsToMatch(
                "/api/events/{eventId}/participants/**",
                "/api/events/{eventId}/groups/**",
            ).build()
    }

    @Bean
    fun eventMovementsApis(): GroupedOpenApi {
        return GroupedOpenApi.builder().group("Event's Movements Management")
            .pathsToMatch("/api/events/{eventId}/movements/**").build()
    }
}
