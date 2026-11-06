package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.model.RoleModel
import reactor.core.publisher.Flux

/**
 * Objectif : Port de lecture des rôles et de leurs permissions.
 * Périmètre : Déclare la lecture des rôles utilisateur et des rôles de projet.
 * Limites : Contrat uniquement ; lecture seule, les rôles sont chargés en mémoire au démarrage.
 */
interface IRolePort {
	fun findUserRoles(): Flux<RoleModel>
	fun findProjectRoles(): Flux<RoleModel>
}
