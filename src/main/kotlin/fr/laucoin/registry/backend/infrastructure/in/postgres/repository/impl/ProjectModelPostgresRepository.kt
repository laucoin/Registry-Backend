package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.port.IProjectPort
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.ProjectEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.IProjectEntityRepository
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class ProjectModelPostgresRepository(
	private val repository: IProjectEntityRepository,
	private val mapper: ProjectEntityMapper,
): IProjectPort {
	override fun findPage(
		pageable: PageableModel,
		searchParams: ProjectSearchParamModel,
	): Mono<PageModel<ProjectModel>> {
		return Mono.zip(
			repository.countAll(
				searchParams.textSearched,
				searchParams.visibilitySearched,
				searchParams.dateTimeSearched,
			),
			repository.findAll(
				searchParams.textSearched,
				searchParams.visibilitySearched,
				searchParams.dateTimeSearched,
				pageable.limit,
				pageable.offset,
			).map(mapper::toModel).collectList(),
		).map {
			PageModel(pageable, it.t1, it.t2)
		}
	}

	override fun findPage(
		projectIds: List<UUID>,
		pageable: PageableModel,
		searchParams: ProjectSearchParamModel
	): Mono<PageModel<ProjectModel>> {
		if (projectIds.isEmpty()) {
			return Mono.just(PageModel(pageable, 0, emptyList()))
		}

		return Mono.zip(
			repository.countAllInProjectIds(
				projectIds,
				searchParams.textSearched,
				searchParams.visibilitySearched,
				searchParams.dateTimeSearched,
			),
			repository.findAllInProjectIds(
				projectIds,
				searchParams.textSearched,
				searchParams.visibilitySearched,
				searchParams.dateTimeSearched,
				pageable.limit,
				pageable.offset,
			).map(mapper::toModel).collectList(),
		).map {
			PageModel(pageable, it.t1, it.t2)
		}
	}

	override fun validDateTime(id: UUID, begin: ZonedDateTime?, end: ZonedDateTime?): Mono<Boolean> {
		return repository.validDateTime(id, begin, end)
			.map { (it.count ?: 0) == 0 }
	}

	override fun findProjectsEligibleForPurge(dateThreshold: LocalDate): Flux<UUID> {
		return repository.findProjectsEligibleForPurge(dateThreshold)
	}

	override fun findById(id: UUID, visibilitySearched: Boolean?): Mono<ProjectModel> {
		return repository.findById(id, visibilitySearched)
			.map(mapper::toModel)
			.switchIfEmpty(Mono.empty())
	}

	override fun create(element: ProjectModel): Mono<ProjectModel> {
		return save(element)
	}

	override fun update(element: ProjectModel): Mono<ProjectModel> {
		return save(element)
	}

	private fun save(element: ProjectModel): Mono<ProjectModel> {
		return repository.save(mapper.toEntity(element)).map(mapper::toModel)
	}

	override fun deleteById(id: UUID): Mono<Void> {
		return repository.deleteById(id)
	}
}
