package fr.laucoin.registry.backend.infrastructure.driving.security.handler

import fr.laucoin.registry.backend.domain.enumeration.AuditOutcomeEnum
import fr.laucoin.registry.backend.domain.enumeration.AuditResourceTypeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.service.IAuditReadService
import fr.laucoin.registry.backend.infrastructure.driving.security.service.ClientContextService
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.HEAD
import org.springframework.http.HttpStatusCode
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.util.AntPathMatcher
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.util.Optional
import java.util.UUID

/**
 * Objectif : Trace dans l'audit les lectures de données personnelles des utilisateurs, des participants et des mouvements.
 * Périmètre : Reconnaît les appels GET et HEAD concernés, et délègue au cas d'usage d'audit une fois la réponse connue : lecture aboutie ou refusée par l'autorisation.
 * Limites : Ne trace ni les requêtes non authentifiées ni les autres statuts ; ne modifie pas la réponse et ne l'attend pas. Doit s'exécuter après l'authentification et avant l'autorisation.
 */
class AuditReadHandler(
	private val auditReadService: IAuditReadService,
	private val clientContextService: ClientContextService,
) : WebFilter {
	private val pathMatcher = AntPathMatcher()

	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		val request = exchange.request
		val target = if (request.method in READ_METHODS) readTarget(request.path.value()) else null
		if (target == null) return chain.filter(exchange)

		return ReactiveSecurityContextHolder.getContext()
			.mapNotNull { it.authentication?.principal as? CurrentUserModel }
			.map { Optional.of(it) }
			.defaultIfEmpty(Optional.empty())
			.flatMap { trace(it.orElse(null), target, exchange, chain) }
	}

	private fun trace(user: CurrentUserModel?, target: ReadTarget, exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		if (user == null) return chain.filter(exchange)

		return chain.filter(exchange).then(Mono.fromRunnable { record(user, target, exchange) })
	}

	private fun record(user: CurrentUserModel, target: ReadTarget, exchange: ServerWebExchange) {
		val outcome = outcome(exchange.response.statusCode) ?: return

		auditReadService.recordRead(
			user, target.type, target.resourceId, target.projectId, clientContextService.resolve(exchange.request), outcome,
		)
	}

	private fun outcome(status: HttpStatusCode?): AuditOutcomeEnum? = when {
		status == null || status.is2xxSuccessful -> AuditOutcomeEnum.SUCCESS
		status.value() == FORBIDDEN_STATUS -> AuditOutcomeEnum.FORBIDDEN
		else -> null
	}

	private fun readTarget(path: String): ReadTarget? {
		return TARGETS.firstNotNullOfOrNull { (type, pattern, hasId) -> match(type, pattern, hasId, path) }
	}

	private fun match(type: AuditResourceTypeEnum, pattern: String, hasId: Boolean, path: String): ReadTarget? {
		if (!pathMatcher.match(pattern, path)) return null

		val variables = pathMatcher.extractUriTemplateVariables(pattern, path)
		val projectId = variables["projectId"]?.toUuid()
		val resourceId = variables["id"]?.toUuid()
		return if ((hasId && resourceId == null) || (variables.containsKey("projectId") && projectId == null)) {
			null
		} else {
			ReadTarget(type, resourceId, projectId)
		}
	}

	private fun String.toUuid(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()

	private data class ReadTarget(val type: AuditResourceTypeEnum, val resourceId: UUID?, val projectId: UUID?)

	private data class TargetPattern(val type: AuditResourceTypeEnum, val pattern: String, val hasId: Boolean)

	private companion object {
		const val FORBIDDEN_STATUS = 403
		val READ_METHODS = setOf(GET, HEAD)
		const val PROJECT = "/api/*/projects/{projectId}"

		val TARGETS = listOf(
			TargetPattern(AuditResourceTypeEnum.USER, "/api/*/users", false),
			TargetPattern(AuditResourceTypeEnum.USER, "/api/*/users/{id}", true),
			TargetPattern(AuditResourceTypeEnum.PARTICIPANT, "$PROJECT/participants", false),
			TargetPattern(AuditResourceTypeEnum.PARTICIPANT, "$PROJECT/participants/birthday", false),
			TargetPattern(AuditResourceTypeEnum.PARTICIPANT, "$PROJECT/participants/search/users", false),
			TargetPattern(AuditResourceTypeEnum.PARTICIPANT, "$PROJECT/participants/{id}", true),
			TargetPattern(AuditResourceTypeEnum.PARTICIPANT, "$PROJECT/participants/{id}/timeline", true),
			TargetPattern(AuditResourceTypeEnum.PARTICIPANT, "$PROJECT/participants/{id}/movements", true),
			TargetPattern(AuditResourceTypeEnum.MOVEMENT, "$PROJECT/movements", false),
			TargetPattern(AuditResourceTypeEnum.MOVEMENT, "$PROJECT/movements/contents", false),
			TargetPattern(AuditResourceTypeEnum.MOVEMENT, "$PROJECT/movements/search/participants-and-groups", false),
			TargetPattern(AuditResourceTypeEnum.MOVEMENT, "$PROJECT/movements/{id}", true),
		)
	}
}
