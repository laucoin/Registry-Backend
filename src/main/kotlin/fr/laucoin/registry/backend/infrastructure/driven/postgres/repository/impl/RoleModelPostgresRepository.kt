package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.RoleModel
import fr.laucoin.registry.backend.domain.port.IRolePort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.RoleEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IRoleEntityRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

/**
 * Objectif : Adaptateur Postgres du port des rôles.
 * Périmètre : Délègue au repository d'entités et mappe les entités vers les modèles du domaine.
 * Limites : Ne porte aucune règle métier.
 */
@Service
class RoleModelPostgresRepository(
	private val repository: IRoleEntityRepository,
	private val mapper: RoleEntityMapper,
): IRolePort {
	override fun findUserRoles(): Flux<RoleModel> {
		return repository.findUserRoles().map(mapper::toModel)
	}

	override fun findProjectRoles(): Flux<RoleModel> {
		return repository.findProjectRoles().map(mapper::toModel)
	}
}
