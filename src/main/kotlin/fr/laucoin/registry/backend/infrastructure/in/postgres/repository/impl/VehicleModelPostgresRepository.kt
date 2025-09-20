package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.port.IVehiclePort
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.VehicleEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.IVehicleEntityRepository
import java.time.LocalDate
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class VehicleModelPostgresRepository(
	private val repository: IVehicleEntityRepository,
	private val mapper: VehicleEntityMapper,
): IVehiclePort {
	override fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: VehicleSearchParamModel,
	): Mono<PageModel<VehicleModel>> {
		return Mono.zip(
			repository.countAll(
				projectId,
				searchParams.textSearched,
				searchParams.visibilitySearched,
				searchParams.availabilitySearched,
				searchParams.presenceSearched,
				searchParams.dateTimeSearched,
			),
			repository.findAll(
				projectId,
				searchParams.textSearched,
				searchParams.visibilitySearched,
				searchParams.availabilitySearched,
				searchParams.presenceSearched,
				searchParams.dateTimeSearched,
				pageable.limit,
				pageable.offset,
			).map(mapper::toModel).collectList()
		).map {
			PageModel(pageable, it.t1, it.t2)
		}
	}

	override fun countAll(
		projectId: UUID,
		searchParams: VehicleSearchParamModel
	): Mono<Long> {
		return repository.countAll(
			projectId,
			searchParams.textSearched,
			searchParams.visibilitySearched,
			searchParams.availabilitySearched,
			searchParams.presenceSearched,
			searchParams.dateTimeSearched,
		)
	}

	override fun findAllByIds(projectId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<VehicleModel> {
		return if (ids.isEmpty()) Flux.empty() else repository.findAllByIds(projectId, ids, visibilitySearched)
			.map(mapper::toModel)
	}

	override fun findWithLimit(limit: Int, projectId: UUID, searchParams: VehicleSearchParamModel): Flux<VehicleModel> {
		return repository.findWithLimit(
			projectId,
			searchParams.textSearched,
			searchParams.visibilitySearched,
			searchParams.availabilitySearched,
			searchParams.presenceSearched,
			searchParams.dateTimeSearched,
			limit,
		).map(mapper::toModel)
	}

	override fun findUnusedSince(dateThreshold: LocalDate): Flux<UUID> {
		return repository.findUnusedSince(dateThreshold)
	}

	override fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<VehicleModel> {
		return repository.findById(projectId, id, visibilitySearched).map(mapper::toModel)
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

	override fun deleteById(id: UUID): Mono<Void> {
		return repository.deleteById(id)
	}
}
