package fr.laucoin.registry.backend.infrastructure.driving.security.chain

import fr.laucoin.registry.backend.domain.service.support.IAuditService
import fr.laucoin.registry.backend.domain.service.support.ITranslateService
import fr.laucoin.registry.backend.infrastructure.driving.security.handler.AuthenticationRateLimitHandler
import fr.laucoin.registry.backend.infrastructure.driving.security.handler.ExportRateLimitHandler
import fr.laucoin.registry.backend.infrastructure.driving.security.handler.ProjectCreationRateLimitHandler
import fr.laucoin.registry.backend.infrastructure.driving.security.handler.UserSearchRateLimitHandler
import fr.laucoin.registry.backend.infrastructure.driving.security.service.ClientContextService
import fr.laucoin.registry.backend.infrastructure.driving.security.service.TooManyRequestsResponder
import org.springframework.core.annotation.Order
import org.springframework.security.config.web.server.SecurityWebFiltersOrder.AUTHENTICATION
import org.springframework.security.config.web.server.SecurityWebFiltersOrder.FIRST
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.stereotype.Component
import org.springframework.web.server.i18n.LocaleContextResolver
import tools.jackson.databind.ObjectMapper
import java.time.Clock

/**
 * Objectif : Limite le débit des appels d'authentification, des exports de données personnelles, de la recherche d'utilisateurs et de la création de projets.
 * Périmètre : Ajoute les filtres de limitation, avec les capacités et fenêtres de la configuration typée.
 * Limites : Ne mesure rien hors des filtres qu'il ajoute.
 */
@Component
@Order(3)
class RateLimitChainCustomizer(
	private val translateService: ITranslateService,
	private val localeContextResolver: LocaleContextResolver,
	private val objectMapper: ObjectMapper,
	private val clock: Clock,
	private val rateLimits: SecurityRateLimitProperties,
	private val auditService: IAuditService,
	private val clientContextService: ClientContextService,
) : SecurityChainCustomizer {
	override fun customize(http: ServerHttpSecurity): ServerHttpSecurity {
		return http
			.addFilterBefore(authenticationRateLimit(), FIRST)
			.addFilterAfter(exportRateLimit(), AUTHENTICATION)
			.addFilterAfter(userSearchRateLimit(), AUTHENTICATION)
			.addFilterAfter(projectCreationRateLimit(), AUTHENTICATION)
	}

	private fun authenticationRateLimit() = AuthenticationRateLimitHandler(
		clientContextService,
		tooManyRequestsResponder(),
		clock,
		rateLimits.auth.capacity,
		rateLimits.auth.windowSeconds,
		rateLimits.auth.maxTrackedClients,
	)

	private fun exportRateLimit() = ExportRateLimitHandler(
		auditService,
		clientContextService,
		tooManyRequestsResponder(),
		clock,
		rateLimits.export.capacity,
		rateLimits.export.windowSeconds,
		rateLimits.export.maxTrackedUsers,
	)

	private fun userSearchRateLimit() = UserSearchRateLimitHandler(
		tooManyRequestsResponder(),
		clock,
		rateLimits.userSearch.capacity,
		rateLimits.userSearch.windowSeconds,
		rateLimits.userSearch.maxTrackedUsers,
	)

	private fun projectCreationRateLimit() = ProjectCreationRateLimitHandler(
		tooManyRequestsResponder(),
		clock,
		rateLimits.projectCreation.capacity,
		rateLimits.projectCreation.windowSeconds,
		rateLimits.projectCreation.maxTrackedUsers,
	)

	private fun tooManyRequestsResponder() =
		TooManyRequestsResponder(translateService, localeContextResolver, objectMapper)
}
