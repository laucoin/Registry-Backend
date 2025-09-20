package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.port.IActivityPort
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.ActivityEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.IActivityEntityRepository
import java.time.LocalDate
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class ActivityModelPostgresRepository(
	private val repository: IActivityEntityRepository,
	private val mapper: ActivityEntityMapper,
): IActivityPort {
	override fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ActivitySearchParamModel,
	): Mono<PageModel<ActivityModel>> {
		return Mono.zip(
			repository.countAll(
				projectId,
				searchParams.textSearched,
				searchParams.visibilitySearched,
				searchParams.availabilitySearched,
				searchParams.dateTimeSearched,
			),
			repository.findAll(
				projectId,
				searchParams.textSearched,
				searchParams.visibilitySearched,
				searchParams.availabilitySearched,
				searchParams.dateTimeSearched,
				pageable.limit,
				pageable.offset,
			).map(mapper::toModel).collectList()
		).map {
			PageModel(pageable, it.t1, it.t2)
		}
	}

	override fun findAllByIds(projectId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<ActivityModel> {
		return if (ids.isEmpty()) Flux.empty() else repository.findAllByIds(projectId, ids, visibilitySearched)
			.map(mapper::toModel)
	}

	override fun findWithLimit(
		limit: Int,
		projectId: UUID,
		searchParams: ActivitySearchParamModel
	): Flux<ActivityModel> {
		return repository.findWithLimit(
			projectId,
			searchParams.textSearched,
			searchParams.visibilitySearched,
			searchParams.availabilitySearched,
			searchParams.dateTimeSearched,
			limit,
		).map(mapper::toModel)
	}

	override fun findUnusedSince(dateThreshold: LocalDate): Flux<UUID> {
		return repository.findUnusedSince(dateThreshold)
	}

	override fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<ActivityModel> {
		return repository.findById(projectId, id, visibilitySearched)
			.map(mapper::toModel)
			.switchIfEmpty(Mono.empty())
	}

	override fun create(element: ActivityModel): Mono<ActivityModel> {
		return save(element)
	}

	override fun update(element: ActivityModel): Mono<ActivityModel> {
		return save(element)
	}

	private fun save(element: ActivityModel): Mono<ActivityModel> {
		return repository.save(mapper.toEntity(element)).map(mapper::toModel)
	}

	override fun deleteById(id: UUID): Mono<Void> {
		return repository.deleteById(id)
	}
}
