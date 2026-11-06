package fr.laucoin.registry.backend.domain.service.support.impl

import fr.laucoin.registry.backend.domain.model.AuditModel
import fr.laucoin.registry.backend.domain.port.IAuditPort
import fr.laucoin.registry.backend.domain.service.support.IAuditService
import java.time.Duration
import java.time.LocalDate
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.EmitResult.FAIL_NON_SERIALIZED

/**
 * Objectif : Enregistre la trace d'audit hors du chemin de la réponse, via une file d'attente bornée.
 * Périmètre : Porte la purge de rétention, la file, son unique consommateur (démarré et drainé avec le cycle de vie de l'application) et la politique de débordement.
 * Limites : File locale à l'instance ; un événement en attente est perdu en cas d'arrêt brutal, et rejeté (journalisé) si la file est pleine.
 */
@Service
class AuditService(
	private val port: IAuditPort,
	@Value($$"${registry.audit.buffer-size}")
	bufferSize: Int,
) : LoggerService(), IAuditService, SmartLifecycle {
	private val sink = Sinks.many().unicast().onBackpressureBuffer<AuditModel>(ArrayBlockingQueue(bufferSize))
	private val running = AtomicBoolean(false)
	private val drained = Sinks.empty<Void>()

	override fun record(audit: AuditModel) {
		if (!offer(audit)) log.error("Audit event {} on {} was dropped", audit.action, audit.resourceType)
	}

	override fun purgeAuditsIfNecessary(deleteOlderThan: LocalDate, dryRun: Boolean): Mono<Long> {
		return port.purgeOlderThan(deleteOlderThan, dryRun)
			.doOnNext { log.info("[dryRun={}] {} audit events older than {} matched", dryRun, it, deleteOlderThan) }
	}

	private fun offer(audit: AuditModel): Boolean {
		repeat(MAX_EMIT_ATTEMPTS) {
			val result = sink.tryEmitNext(audit)
			if (result != FAIL_NON_SERIALIZED) return result.isSuccess
		}
		return false
	}

	private fun write(audit: AuditModel): Mono<Unit> {
		return port.record(audit)
			.onErrorResume {
				log.error("Audit event {} on {} could not be written", audit.action, audit.resourceType, it)
				Mono.empty()
			}
	}

	override fun start() {
		if (running.compareAndSet(false, true)) {
			sink.asFlux()
				.concatMap(::write)
				.doOnTerminate { drained.tryEmitEmpty() }
				.subscribe()
		}
	}

	override fun stop(callback: Runnable) {
		if (!running.getAndSet(false)) return callback.run()

		sink.tryEmitComplete()
		drained.asMono()
			.timeout(Duration.ofSeconds(DRAIN_TIMEOUT_SECONDS))
			.onErrorResume { warnNotDrained() }
			.doFinally { callback.run() }
			.subscribe()
	}

	private fun warnNotDrained(): Mono<Void> {
		log.warn("Audit queue was not fully drained before shutdown")
		return Mono.empty()
	}

	override fun stop() = stop {}

	override fun isRunning(): Boolean = running.get()

	private companion object {
		const val MAX_EMIT_ATTEMPTS = 16
		const val DRAIN_TIMEOUT_SECONDS = 5L
	}
}
