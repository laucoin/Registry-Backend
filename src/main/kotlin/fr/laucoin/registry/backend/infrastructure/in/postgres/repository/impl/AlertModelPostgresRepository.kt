package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.port.IAlertPort
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.AlertEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.IAlertEntityRepository
import java.time.LocalDate
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class AlertModelPostgresRepository(
	private val repository: IAlertEntityRepository,
	private val mapper: AlertEntityMapper,
): IAlertPort {
	override fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: AlertSearchParamModel
	): Mono<PageModel<AlertModel>> {
		return Mono.zip(
			repository.countAll(
				projectId,
				searchParams.textSearched,
				searchParams.statusSearched,
				searchParams.visibilitySearched,
				searchParams.startDateTimeSearched,
				searchParams.endDateTimeSearched,
			),
			repository.findAll(
				projectId,
				searchParams.textSearched,
				searchParams.statusSearched,
				searchParams.visibilitySearched,
				searchParams.startDateTimeSearched,
				searchParams.endDateTimeSearched,
				pageable.limit,
				pageable.offset,
			).map(mapper::toModel).collectList()
		).map {
			PageModel(pageable, it.t1, it.t2)
		}
	}

	override fun findWithLimit(
		limit: Int,
		projectId: UUID,
		searchParams: AlertSearchParamModel
	): Flux<AlertModel> {
		return repository.findWithLimit(
			projectId,
			searchParams.textSearched,
			searchParams.statusSearched,
			searchParams.visibilitySearched,
			searchParams.startDateTimeSearched,
			searchParams.endDateTimeSearched,
			limit,
		).map(mapper::toModel)
	}

	override fun findOlderThanAndUncommentedSince(dateThreshold: LocalDate): Flux<UUID> {
		return repository.findOlderThanAndUncommentedSince(dateThreshold)
	}

	override fun findById(
		projectId: UUID,
		id: UUID,
		visibilitySearched: Boolean?
	): Mono<AlertModel> {
		return repository.findById(
			projectId,
			id,
			visibilitySearched,
		).map(mapper::toModel)
	}

	override fun create(element: AlertModel): Mono<AlertModel> {
		return save(element)
	}

	override fun update(element: AlertModel): Mono<AlertModel> {
		return save(element)
	}

	private fun save(element: AlertModel): Mono<AlertModel> {
		return repository.save(mapper.toEntity(element)).map(mapper::toModel)
	}

	override fun deleteById(id: UUID): Mono<Void> {
		return repository.deleteById(id)
	}
}
