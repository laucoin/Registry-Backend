package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.toPageModel
import fr.laucoin.registry.backend.domain.port.IAlertPort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.AlertEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IAlertEntityRepository
import java.time.LocalDate
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Adaptateur Postgres du port de l'alerte.
 * Périmètre : Délègue au repository d'entités et mappe les entités vers les modèles du domaine.
 * Limites : Ne porte aucune règle métier.
 */
@Service
class AlertModelPostgresRepository(
	private val repository: IAlertEntityRepository,
	private val mapper: AlertEntityMapper,
	@param:Value($$"${registry.feature.alert.criticality.reinforced-after-hours}")
	private val reinforcedAfterHours: Long,
	@param:Value($$"${registry.feature.alert.criticality.high-after-hours}")
	private val highAfterHours: Long,
): IAlertPort {
	override fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: AlertSearchParamModel
	): Mono<PageModel<AlertModel>> {
		return repository.findAll(
			projectId,
			searchParams.query,
			searchParams.status,
			searchParams.criticality,
			reinforcedAfterHours,
			highAfterHours,
			searchParams.isVisible,
			searchParams.from,
			searchParams.to,
			searchParams.sort?.field,
			searchParams.sort?.isDescending,
			pageable.limit,
			pageable.offset,
		).toPageModel(pageable, AlertEntity::fullCount, mapper::toModel)
	}

	override fun findWithLimit(
		limit: Int,
		projectId: UUID,
		searchParams: AlertSearchParamModel
	): Flux<AlertModel> {
		return repository.findWithLimit(
			projectId,
			searchParams.query,
			searchParams.status,
			searchParams.isVisible,
			searchParams.from,
			searchParams.to,
			limit,
		).map(mapper::toModel)
	}

	override fun findOlderThanAndUncommentedSince(dateThreshold: LocalDate): Flux<UUID> {
		return repository.findOlderThanAndUncommentedSince(dateThreshold)
	}

	override fun findById(
		projectId: UUID,
		id: UUID,
		isVisible: Boolean?
	): Mono<AlertModel> {
		return repository.findById(
			projectId,
			id,
			isVisible,
			reinforcedAfterHours,
			highAfterHours,
		).map(mapper::toModel)
	}

	override fun create(element: AlertModel): Mono<AlertModel> {
		return save(element)
	}

	override fun update(element: AlertModel): Mono<AlertModel> {
		return save(element)
	}

	private fun save(element: AlertModel): Mono<AlertModel> {
		return repository.save(mapper.toEntity(element))
			.flatMap { repository.findById(it.projectId!!, it.id!!, null, reinforcedAfterHours, highAfterHours) }
			.map(mapper::toModel)
	}

	override fun deleteById(id: UUID): Mono<Unit> {
		return repository.deleteById(id).thenReturn(Unit)
	}
}
