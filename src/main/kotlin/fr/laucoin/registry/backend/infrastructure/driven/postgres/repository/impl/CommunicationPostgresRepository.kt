package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.toPageModel
import fr.laucoin.registry.backend.domain.port.ICommunicationPort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.communication.CommunicationEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.CommunicationEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.ICommunicationEntityRepository
import java.time.Clock
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Adaptateur Postgres du port de la communication.
 * Périmètre : Délègue au repository d'entités et mappe les entités vers les modèles du domaine.
 * Limites : Ne porte aucune règle métier.
 */
@Service
class CommunicationPostgresRepository(
	private val repository: ICommunicationEntityRepository,
	private val mapper: CommunicationEntityMapper,
	private val clock: Clock,
): ICommunicationPort {
	override fun findByMovementIdsWithLimit(
		limit: Int,
		projectId: UUID,
		movementIds: List<UUID>,
		isVisible: Boolean?,
	): Flux<Pair<UUID, List<CommunicationModel>>> {
		return if (movementIds.isEmpty()) Flux.empty()
		else repository.findAllByMovementIdsWithLimit(projectId, movementIds, isVisible, limit)
			.groupBy { it.movementId!! }
			.flatMap {
				it.collectList().map { list -> it.key() to list.map(mapper::toModel) }
			}
	}

	override fun findPageByMovementId(
		projectId: UUID,
		movementId: UUID,
		pageable: PageableModel,
		searchParams: CommunicationSearchParamModel
	): Mono<PageModel<CommunicationModel>> {
		return repository.findAllByMovementId(
			projectId,
			movementId,
			searchParams.query,
			searchParams.isVisible,
			searchParams.from,
			searchParams.to,
			searchParams.sort?.field,
			searchParams.sort?.isDescending,
			pageable.limit,
			pageable.offset,
		).toPageModel(pageable, clock, CommunicationEntity::fullCount, mapper::toModel)
	}

	override fun findByAlertIdsWithLimit(
		limit: Int,
		projectId: UUID,
		alertIds: List<UUID>,
		isVisible: Boolean?
	): Flux<Pair<UUID, List<CommunicationModel>>> {
		return if (alertIds.isEmpty()) Flux.empty()
		else repository.findAllByAlertIdsWithLimit(projectId, alertIds, isVisible, limit)
			.groupBy { it.movementId!! }
			.flatMap {
				it.collectList().map { list -> it.key() to list.map(mapper::toModel) }
			}
	}

	override fun findPageByAlertId(
		projectId: UUID,
		alertId: UUID,
		pageable: PageableModel,
		searchParams: CommunicationSearchParamModel
	): Mono<PageModel<CommunicationModel>> {
		return repository.findAllByAlertId(
			projectId,
			alertId,
			searchParams.query,
			searchParams.isVisible,
			searchParams.from,
			searchParams.to,
			searchParams.sort?.field,
			searchParams.sort?.isDescending,
			pageable.limit,
			pageable.offset,
		).toPageModel(pageable, clock, CommunicationEntity::fullCount, mapper::toModel)
	}

	override fun findAllByIds(
		projectId: UUID,
		ids: List<UUID>,
		isVisible: Boolean?
	): Flux<CommunicationModel> {
		return if (ids.isEmpty()) Flux.empty() else repository.findAllByIds(projectId, ids, isVisible)
			.map(mapper::toModel)
	}

	override fun countAllByMovementId(
		projectId: UUID,
		movementId: UUID,
		searchParams: CommunicationSearchParamModel
	): Mono<Long> {
		return repository.countAllByMovementId(
			projectId,
			movementId,
			searchParams.query,
			searchParams.isVisible,
			searchParams.from,
			searchParams.to,
		)
	}

	override fun countAllByAlertId(
		projectId: UUID,
		alertId: UUID,
		searchParams: CommunicationSearchParamModel
	): Mono<Long> {
		return repository.countAllByAlertId(
			projectId,
			alertId,
			searchParams.query,
			searchParams.isVisible,
			searchParams.from,
			searchParams.to,
		)
	}

	override fun findOrphan(
		movementsToExclude: List<UUID>,
		alertsToExclude: List<UUID>,
	): Flux<UUID> {
		return when {
			movementsToExclude.isEmpty() && alertsToExclude.isEmpty() -> repository.findOrphan()
			alertsToExclude.isEmpty() -> repository.findOrphanExcludingMovements(movementsToExclude)
			movementsToExclude.isEmpty() -> repository.findOrphanExcludingAlerts(alertsToExclude)
			else -> repository.findOrphanExcludingMovementsAndAlerts(movementsToExclude, alertsToExclude)
		}
	}

	override fun findById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<CommunicationModel> {
		return repository.findById(projectId, id, isVisible)
			.map(mapper::toModel)
			.switchIfEmpty(Mono.empty())
	}

	override fun create(element: CommunicationModel): Mono<CommunicationModel> {
		return save(element)
	}

	override fun update(element: CommunicationModel): Mono<CommunicationModel> {
		return save(element)
	}

	private fun save(element: CommunicationModel): Mono<CommunicationModel> {
		return repository.save(mapper.toEntity(element)).map(mapper::toModel)
	}

	override fun deleteById(id: UUID): Mono<Unit> {
		return repository.deleteById(id).thenReturn(Unit)
	}
}
