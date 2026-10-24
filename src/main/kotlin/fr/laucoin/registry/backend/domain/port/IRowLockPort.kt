package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum
import java.util.UUID
import reactor.core.publisher.Mono

/**
 * Objectif : Prend un verrou pessimiste sur la ligne d'une ressource pour la durée de la transaction courante.
 * Périmètre : Verrouille la ligne d'une ressource verrouillable par id ; une ligne absente n'est pas une erreur.
 * Limites : Ne lit pas la ligne et ne libère jamais le verrou lui-même, qui prend fin avec la transaction.
 */
interface IRowLockPort {
	fun lock(resource: LockableResourceEnum, id: UUID): Mono<Unit>
}
