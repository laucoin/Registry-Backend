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
	@param:Value($$"${registry.security.oauth2.url:}/auth")
	private val configAuthUrl: String?,
	@param:Value($$"${registry.security.oauth2.url:}/token")
	private val configTokenUrl: String?,
	@param:Value($$"${registry.server.prefix:''}")
	private val configApiPrefix: String,
) {
	private companion object {
		private const val CLIENT_NAME = "OAuth2"
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
							.flows(
								OAuthFlows()
									.implicit(
										OAuthFlow().apply {
											authorizationUrl = configAuthUrl
											refreshUrl = configTokenUrl
											tokenUrl = configTokenUrl
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
			.pathsToMatch("/api/authentication/**").build()
	}

	@Bean
	fun metadataApis(): GroupedOpenApi {
		return GroupedOpenApi.builder().group("metadata")
			.pathsToMatch("/api/metadata/**").build()
	}

	@Bean
	fun usersApis(): GroupedOpenApi {
		return GroupedOpenApi.builder().group("users")
			.pathsToMatch("/api/users/**").build()
	}

	@Bean
	fun projectsApis(): GroupedOpenApi {
		return GroupedOpenApi.builder().group("projects")
			.pathsToMatch(
				"/api/projects",
				"/api/projects/{id}",
				"/api/projects/{id}/disable",
				"/api/projects/{id}/enable",
			).build()
	}

	@Bean
	fun projectProfilesApis(): GroupedOpenApi {
		return GroupedOpenApi.builder().group("project-profiles")
			.pathsToMatch("/api/projects/{projectId}/profiles/**").build()
	}

	@Bean
	fun projectParticipantsApis(): GroupedOpenApi {
		return GroupedOpenApi.builder().group("project-participants")
			.pathsToMatch(
				"/api/projects/{projectId}/participants/**",
				"/api/projects/{projectId}/groups/**",
			).build()
	}

	@Bean
	fun projectMovementsApis(): GroupedOpenApi {
		return GroupedOpenApi.builder().group("project-movements")
			.pathsToMatch("/api/projects/{projectId}/movements/**").build()
	}

	@Bean
	fun projectVehiclesApis(): GroupedOpenApi {
		return GroupedOpenApi.builder().group("project-vehicles")
			.pathsToMatch("/api/projects/{projectId}/vehicles/**").build()
	}

	@Bean
	fun projectActivitiesApis(): GroupedOpenApi {
		return GroupedOpenApi.builder().group("project-activities")
			.pathsToMatch("/api/projects/{projectId}/activities/**").build()
	}

	@Bean
	fun projectCommunicationsApis(): GroupedOpenApi {
		return GroupedOpenApi.builder().group("project-communications")
			.pathsToMatch("/api/projects/{projectId}/communications/**").build()
	}

	@Bean
	fun projectAlertsApis(): GroupedOpenApi {
		return GroupedOpenApi.builder().group("project-alerts")
			.pathsToMatch("/api/projects/{projectId}/alerts/**").build()
	}

	@Bean
	fun purgesApis(): GroupedOpenApi {
		return GroupedOpenApi.builder().group("data-purges")
			.pathsToMatch("/api/purge/**").build()
	}
}
