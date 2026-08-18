package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.AuditConst.CORRELATION_ID_CONTEXT_KEY
import fr.laucoin.registry.backend.domain.constant.AuditConst.OUTCOME_CANCELLED
import fr.laucoin.registry.backend.domain.constant.AuditConst.OUTCOME_FAILURE_PREFIX
import fr.laucoin.registry.backend.domain.constant.AuditConst.OUTCOME_SUCCESS
import fr.laucoin.registry.backend.domain.enumeration.AuditActionEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.service.IAuditService
import fr.laucoin.registry.backend.domain.service.impl.AuditService.Companion.AUDIT_LOGGER
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.publisher.SignalType.CANCEL
import java.time.ZonedDateTime
import java.util.Objects

/**
 * Append-only audit emission, separate from application logs: a
 * dedicated logger name ([AUDIT_LOGGER]) carries one structured line per
 * privileged action, so the stream can be routed/retained independently (SIEM
 * shipping and a durable sink are deployment concerns on this logger name).
 * Each line is a single flat JSON object with the string fields "timestamp",
 * "actorSub", "actorId", "action", "targetId", "outcome" and "correlationId"
 * (null fields omitted), produced by a local deterministic writer so the
 * security-critical trail never depends on a third party's shaded internals.
 * Emission is non-blocking and never contains secrets or token material.
 * Cancellation of the wrapped pipeline (e.g. a client disconnect while the
 * dispatched mutation may still complete) is stamped [OUTCOME_CANCELLED] via
 * doFinally, which fires exactly once per subscription, so success/failure
 * semantics are preserved and no entry is double-recorded.
 */
@Service
class AuditService : IAuditService {
	private companion object {
		private const val AUDIT_LOGGER = "fr.laucoin.registry.audit"
	}

	private val auditLog = LoggerFactory.getLogger(AUDIT_LOGGER)
	private val log = LoggerFactory.getLogger(AuditService::class.java)

	override fun <T : Any> audit(
		source: Mono<T>,
		actor: CurrentUserModel,
		action: AuditActionEnum,
		targetId: Any?,
	): Mono<T> {
		return Mono.deferContextual { context ->
			val correlationId: String? = context.getOrDefault(CORRELATION_ID_CONTEXT_KEY, null)
			source
				.doOnSuccess {
					record(
						actor,
						action,
						targetId,
						outcome = OUTCOME_SUCCESS,
						correlationId = correlationId
					)
				}
				.doOnError {
					record(
						actor,
						action,
						targetId,
						outcome = "$OUTCOME_FAILURE_PREFIX${it::class.simpleName}",
						correlationId = correlationId,
					)
				}
				.doFinally {
					if (it == CANCEL) {
						record(actor, action, targetId, outcome = OUTCOME_CANCELLED, correlationId = correlationId)
					}
				}
		}
	}

	/**
	 * Best-effort emission: record() runs inside doOnSuccess/doOnError/doFinally,
	 * where a thrown exception would replace the pipeline's outcome — a failed
	 * audit write must never turn a successful operation into an API error.
	 */
	private fun record(
		actor: CurrentUserModel,
		action: AuditActionEnum,
		targetId: Any?,
		outcome: String,
		correlationId: String?,
	) {
		try {
			auditLog.info(
				toJsonLine(
					mapOf(
						"timestamp" to ZonedDateTime.now().toString(),
						"actorSub" to actor.oidcId?.toString(),
						"actorId" to actor.id?.toString(),
						"action" to action.name,
						"targetId" to targetId?.toString(),
						"outcome" to outcome,
						"correlationId" to correlationId,
					)
				)
			)
		} catch (e: Exception) {
			log.error("Failed to emit audit entry for action {} on target {}", action, targetId, e)
		}
	}

	/**
	 * Minimal deterministic JSON writer for the flat string map above, kept
	 * local so the trail has no dependency on a shaded third-party serializer;
	 * entries keep insertion order and null values are omitted.
	 */
	private fun toJsonLine(fields: Map<String, String?>): String {
		return fields.entries
			.filter { Objects.nonNull(it.value) }
			.joinToString(separator = ",", prefix = "{", postfix = "}") {
				"\"${it.key}\":\"${escapeJson(it.value!!)}\""
			}
	}

	private fun escapeJson(value: String): String = buildString(value.length) {
		value.forEach {
			when {
				it == '"' -> append("\\\"")
				it == '\\' -> append("\\\\")
				it < ' ' -> append("\\u%04x".format(it.code))
				else -> append(it)
			}
		}
	}
}
