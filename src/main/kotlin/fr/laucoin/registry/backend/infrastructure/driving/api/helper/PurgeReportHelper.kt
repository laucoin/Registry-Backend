package fr.laucoin.registry.backend.infrastructure.driving.api.helper

import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PURGE_REPORTED_IDS_MAX
import java.util.UUID
import reactor.core.publisher.Flux

/**
 * Objectif : Plafonne la liste des identifiants signalés dans la réponse d'une purge.
 * Périmètre : Garde au plus le nombre configuré d'identifiants, pour les listes comme pour les flux de purge.
 * Limites : Ne limite jamais la purge elle-même : le flux est entièrement consommé avant d'être plafonné, car l'annuler arrêterait les suppressions.
 */
object PurgeReportHelper {
	fun List<UUID>.reported(): List<UUID> = take(PURGE_REPORTED_IDS_MAX)

	fun Flux<UUID>.reportedOnceCompleted(): Flux<UUID> = collectList().flatMapIterable { it.reported() }
}
