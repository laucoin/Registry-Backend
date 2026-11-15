package fr.laucoin.registry.backend.infrastructure.driving.security.service

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

/**
 * Objectif : Compte les appels d'un client sur une fenêtre de temps et décide s'ils restent dans la capacité.
 * Périmètre : Porte le compteur atomique sans verrou, remis à zéro à l'expiration de la fenêtre, selon l'horloge injectée.
 * Limites : Compteur en mémoire pour une seule clé ; le suivi des clés et la réponse de refus relèvent de l'appelant.
 */
class SlidingWindowCounter(private val clock: Clock, private val capacity: Int, private val window: Duration) {
	private val state = AtomicReference(WindowState(clock.instant(), 0))

	fun tryConsume(): Boolean {
		while (true) {
			val current = state.get()
			val now = clock.instant()
			val next = if (Duration.between(current.start, now) >= window) {
				WindowState(now, 1)
			} else {
				WindowState(current.start, current.count + 1)
			}
			if (state.compareAndSet(current, next)) {
				return next.count <= capacity
			}
		}
	}

	private data class WindowState(val start: Instant, val count: Int)
}
