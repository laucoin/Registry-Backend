package fr.laucoin.registry.backend.domain.service.usecase

import java.time.LocalDate
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat des purges de rétention des utilisateurs, des projets et des événements d'audit.
 * Périmètre : Déclare les trois balayages indépendants de la plateforme ; chacun accepte une date seuil et un mode simulation.
 * Limites : Contrat uniquement ; le mode simulation est vrai par défaut côté API.
 */
interface IPlatformPurgeUseCase {
	fun purgeUsers(dateThreshold: LocalDate?, dryRun: Boolean): Flux<UUID>
	fun purgeProjects(dateThreshold: LocalDate?, dryRun: Boolean): Flux<UUID>
	fun purgeAudits(dateThreshold: LocalDate?, dryRun: Boolean): Mono<Long>
}
