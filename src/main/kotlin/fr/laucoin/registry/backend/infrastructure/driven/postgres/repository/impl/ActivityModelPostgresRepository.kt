package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.toPageModel
import fr.laucoin.registry.backend.domain.port.IActivityPort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.activity.ActivityEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.ActivityEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IActivityEntityRepository
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Adaptateur Postgres du port de l'activité.
 * Périmètre : Délègue au repository d'entités et mappe les entités vers les modèles du domaine.
 * Limites : Ne porte aucune règle métier.
 */
@Service
class ActivityModelPostgresRepository(
	private val repository: IActivityEntityRepository,
	private val mapper: ActivityEntityMapper,
	private val clock: Clock,
): IActivityPort {
	override fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ActivitySearchParamModel,
	): Mono<PageModel<ActivityModel>> {
		return repository.findAll(
			projectId,
			searchParams.query,
			searchParams.isVisible,
			searchParams.isAvailable,
			searchParams.at,
			searchParams.minDuration?.toIsoString(),
			searchParams.maxDuration?.toIsoString(),
			searchParams.participants,
			searchParams.sort?.field,
			searchParams.sort?.isDescending,
			pageable.limit,
			pageable.offset,
		).toPageModel(pageable, clock, ActivityEntity::fullCount, mapper::toModel)
	}

	override fun findAllByIds(projectId: UUID, ids: List<UUID>, isVisible: Boolean?): Flux<ActivityModel> {
		return if (ids.isEmpty()) Flux.empty() else repository.findAllByIds(projectId, ids, isVisible)
			.map(mapper::toModel)
	}

	override fun findWithLimit(
		limit: Int,
		projectId: UUID,
		searchParams: ActivitySearchParamModel
	): Flux<ActivityModel> {
		return repository.findWithLimit(
			projectId,
			searchParams.query,
			searchParams.isVisible,
			searchParams.isAvailable,
			searchParams.at,
			searchParams.minDuration?.toIsoString(),
			searchParams.maxDuration?.toIsoString(),
			searchParams.participants,
			limit,
		).map(mapper::toModel)
	}

	override fun findUnusedSince(dateThreshold: LocalDate): Flux<UUID> {
		return repository.findUnusedSince(dateThreshold)
	}

	override fun findById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<ActivityModel> {
		return repository.findById(projectId, id, isVisible)
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

	override fun deleteById(id: UUID): Mono<Unit> {
		return repository.deleteById(id).thenReturn(Unit)
	}
}
