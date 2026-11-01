package fr.laucoin.registry.backend.infrastructure.driving.security.handler

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import fr.laucoin.registry.backend.domain.enumeration.AuditActionEnum.EXPORT
import fr.laucoin.registry.backend.domain.enumeration.AuditOutcomeEnum.RATE_LIMITED
import fr.laucoin.registry.backend.domain.enumeration.AuditResourceTypeEnum
import fr.laucoin.registry.backend.domain.model.AuditModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.service.IAuditService
import fr.laucoin.registry.backend.infrastructure.driving.security.service.ClientContextService
import fr.laucoin.registry.backend.infrastructure.driving.security.service.SlidingWindowCounter
import fr.laucoin.registry.backend.infrastructure.driving.security.service.TooManyRequestsResponder
import java.time.Clock
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Optional
import java.util.UUID
import org.springframework.http.HttpMethod.GET
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.util.AntPathMatcher
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * Objectif : Limite fortement le débit des exports de données personnelles pour empêcher une extraction massive.
 * Périmètre : Compte les appels GET d'export par utilisateur authentifié, tous endpoints d'export confondus, sur une fenêtre de temps ; répond 429 au-delà de la capacité et en trace le refus dans l'audit.
 * Limites : En mémoire et par instance ; le nombre d'utilisateurs suivis est borné. Doit s'exécuter après l'authentification ; les requêtes non authentifiées lui échappent et sont refusées en aval.
 */
class ExportRateLimitHandler(
	private val auditService: IAuditService,
	private val clientContextService: ClientContextService,
	private val responder: TooManyRequestsResponder,
	private val clock: Clock,
	private val capacity: Int,
	private val windowSeconds: Long,
	maxTrackedUsers: Long,
) : WebFilter {
	private val pathMatcher = AntPathMatcher()

	private val counters: Cache<UUID, SlidingWindowCounter> = Caffeine.newBuilder()
		.expireAfterAccess(Duration.ofSeconds(windowSeconds * EXPIRY_GRACE_MULTIPLIER))
		.maximumSize(maxTrackedUsers)
		.build()

	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		val request = exchange.request
		val target = if (request.method == GET) exportTarget(request.path.value()) else null
		if (target == null) return chain.filter(exchange)

		return ReactiveSecurityContextHolder.getContext()
			.mapNotNull { (it.authentication?.principal as? CurrentUserModel)?.id }
			.map { Optional.of(it) }
			.defaultIfEmpty(Optional.empty())
			.flatMap { limit(it.orElse(null), target, exchange, chain) }
	}

	private fun limit(userId: UUID?, target: ExportTarget, exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		if (userId == null || consume(userId)) return chain.filter(exchange)

		auditService.record(refusal(userId, target, exchange))
		return responder.respond(exchange, windowSeconds)
	}

	private fun consume(userId: UUID): Boolean {
		return counters.get(userId) { SlidingWindowCounter(clock, capacity, Duration.ofSeconds(windowSeconds)) }.tryConsume()
	}

	private fun refusal(userId: UUID, target: ExportTarget, exchange: ServerWebExchange): AuditModel {
		return AuditModel(
			action = EXPORT,
			resourceType = target.type,
			resourceId = if (target.resourceId == CURRENT) userId else target.resourceId.toUuid(),
			projectId = target.projectId.toUuid(),
			actorUserId = userId,
			outcome = RATE_LIMITED,
			client = clientContextService.resolve(exchange.request),
			occurredAt = ZonedDateTime.now(clock),
		)
	}

	private fun String?.toUuid(): UUID? = this?.let { runCatching { UUID.fromString(it) }.getOrNull() }

	private fun exportTarget(path: String): ExportTarget? {
		return when {
			pathMatcher.match(USER_EXPORT, path) -> pathMatcher.extractUriTemplateVariables(USER_EXPORT, path)
				.let { ExportTarget(AuditResourceTypeEnum.USER, it["id"], null) }
			pathMatcher.match(PARTICIPANT_EXPORT, path) -> pathMatcher.extractUriTemplateVariables(PARTICIPANT_EXPORT, path)
				.let { ExportTarget(AuditResourceTypeEnum.PARTICIPANT, it["id"], it["projectId"]) }
			else -> null
		}
	}

	private data class ExportTarget(val type: AuditResourceTypeEnum, val resourceId: String?, val projectId: String?)

	private companion object {
		const val USER_EXPORT = "/api/*/users/{id}/export"
		const val PARTICIPANT_EXPORT = "/api/*/projects/{projectId}/participants/{id}/export"
		const val CURRENT = "current"
		const val EXPIRY_GRACE_MULTIPLIER = 10L
	}
}
