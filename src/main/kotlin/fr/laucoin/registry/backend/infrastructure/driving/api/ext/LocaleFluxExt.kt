package fr.laucoin.registry.backend.infrastructure.driving.api.ext

import io.micrometer.context.ContextRegistry
import io.micrometer.context.ContextSnapshotFactory
import org.springframework.context.i18n.LocaleContextThreadLocalAccessor
import reactor.core.publisher.Flux
import reactor.util.context.ContextView
import reactor.util.context.ReactorContextAccessor

/**
 * Objectif : Traduit les éléments d'un flux dans la locale de la requête.
 * Périmètre : Restaure la locale du contexte réactif dans son ThreadLocal le temps de chaque conversion, quel que soit le thread qui émet.
 * Limites : Ne restaure que la locale ; les flux issus de la base la retrouvent déjà par la propagation automatique.
 */
object LocaleFluxExt {
	private val registry: ContextRegistry = ContextRegistry()
		.registerContextAccessor(ReactorContextAccessor())
		.registerThreadLocalAccessor(LocaleContextThreadLocalAccessor())
	private val snapshots: ContextSnapshotFactory = ContextSnapshotFactory.builder().contextRegistry(registry).build()

	fun <T : Any, R : Any> Flux<T>.mapInRequestLocale(mapper: (T) -> R): Flux<R> =
		handle { element, sink ->
			snapshots.setThreadLocalsFrom<ContextView>(sink.contextView(), LocaleContextThreadLocalAccessor.KEY)
				.use { sink.next(mapper(element)) }
		}
}
