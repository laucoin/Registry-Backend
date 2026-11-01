package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import java.time.Clock
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.toPageModel
import fr.laucoin.registry.backend.domain.port.IVehiclePort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.vehicle.VehicleEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.VehicleEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IVehicleEntityRepository
import java.time.LocalDate
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Adaptateur Postgres du port du véhicule.
 * Périmètre : Délègue au repository d'entités et mappe les entités vers les modèles du domaine.
 * Limites : Ne porte aucune règle métier.
 */
@Service
class VehicleModelPostgresRepository(
	private val repository: IVehicleEntityRepository,
	private val mapper: VehicleEntityMapper,
	private val clock: Clock,
): IVehiclePort {
	override fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: VehicleSearchParamModel,
	): Mono<PageModel<VehicleModel>> {
		return repository.findAll(
			projectId,
			searchParams.query,
			searchParams.isVisible,
			searchParams.isAvailable,
			searchParams.isPresent,
			searchParams.at,
			searchParams.brand,
			searchParams.model,
			searchParams.sort?.field,
			searchParams.sort?.isDescending,
			pageable.limit,
			pageable.offset,
		).toPageModel(pageable, clock, VehicleEntity::fullCount, mapper::toModel)
	}

	override fun countAll(
		projectId: UUID,
		searchParams: VehicleSearchParamModel
	): Mono<Long> {
		return repository.countAll(
			projectId,
			searchParams.query,
			searchParams.isVisible,
			searchParams.isAvailable,
			searchParams.isPresent,
			searchParams.at,
			searchParams.brand,
			searchParams.model,
		)
	}

	override fun findAllByIds(projectId: UUID, ids: List<UUID>, isVisible: Boolean?): Flux<VehicleModel> {
		return if (ids.isEmpty()) Flux.empty() else repository.findAllByIds(projectId, ids, isVisible)
			.map(mapper::toModel)
	}

	override fun findWithLimit(limit: Int, projectId: UUID, searchParams: VehicleSearchParamModel): Flux<VehicleModel> {
		return repository.findWithLimit(
			projectId,
			searchParams.query,
			searchParams.isVisible,
			searchParams.isAvailable,
			searchParams.isPresent,
			searchParams.at,
			searchParams.brand,
			searchParams.model,
			limit,
		).map(mapper::toModel)
	}

	override fun findUnusedSince(dateThreshold: LocalDate): Flux<UUID> {
		return repository.findUnusedSince(dateThreshold)
	}

	override fun findById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<VehicleModel> {
		return repository.findById(projectId, id, isVisible).map(mapper::toModel)
	}

	override fun create(element: VehicleModel): Mono<VehicleModel> {
		return save(element)
	}

	override fun update(element: VehicleModel): Mono<VehicleModel> {
		return save(element)
	}

	private fun save(element: VehicleModel): Mono<VehicleModel> {
		return repository.save(mapper.toEntity(element)).map(mapper::toModel)
	}

	override fun deleteById(id: UUID): Mono<Unit> {
		return repository.deleteById(id).thenReturn(Unit)
	}
}
