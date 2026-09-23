package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.ParticipantSortFieldEnum
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.toPageModel
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.GroupContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.ParticipantEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GroupContentJooqRepository
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.ParticipantJooqRepository
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

@Service
class ParticipantModelPostgresRepository(
	private val repository: ParticipantJooqRepository,
	private val groupContentRepository: GroupContentJooqRepository,
	private val dsl: DSLContext,
	private val mapper: ParticipantEntityMapper,
	private val groupContentMapper: GroupContentEntityMapper,
) : IParticipantPort {
	override fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ParticipantSearchParamModel,
		sortFields: List<SortModel<ParticipantSortFieldEnum>>,
	): Mono<PageModel<ParticipantModel>> {
		return repository.findAll(
			projectId,
			searchParams.textSearched,
			searchParams.isMajor,
			searchParams.typeSearched,
			searchParams.visibilitySearched,
			searchParams.availabilitySearched,
			searchParams.presenceSearched,
			searchParams.dateTimeSearched,
			sortFields,
			pageable.limit,
			pageable.offset,
		).toPageModel(pageable, ParticipantEntity::fullCount, mapper::toModel)
	}

	override fun findBirthdays(projectId: UUID, visibilitySearched: Boolean?): Flux<ParticipantModel> {
		return repository.findAllWithBirthday(projectId, visibilitySearched)
			.map(mapper::toModel)
	}

	override fun countAll(
		projectId: UUID,
		searchParams: ParticipantSearchParamModel
	): Mono<Long> {
		return repository.countAll(
			projectId,
			searchParams.textSearched,
			searchParams.isMajor,
			searchParams.typeSearched,
			searchParams.visibilitySearched,
			searchParams.availabilitySearched,
			searchParams.presenceSearched,
			searchParams.dateTimeSearched,
		)
	}

	override fun findPageByGroupId(
		projectId: UUID,
		groupId: UUID,
		pageable: PageableModel,
		searchParams: ParticipantSearchParamModel
	): Mono<PageModel<ParticipantModel>> {
		return repository.findAllByGroupId(
			projectId,
			groupId,
			searchParams.textSearched,
			searchParams.isMajor,
			searchParams.typeSearched,
			searchParams.visibilitySearched,
			searchParams.availabilitySearched,
			searchParams.presenceSearched,
			searchParams.dateTimeSearched,
			pageable.limit,
			pageable.offset,
		).toPageModel(pageable, ParticipantEntity::fullCount, mapper::toModel)
	}

	override fun findAllByIds(projectId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<ParticipantModel> {
		return if (ids.isEmpty()) Flux.empty()
		else repository.findAllByIds(projectId, ids, visibilitySearched, dateTimeSearched = null).map(mapper::toModel)
	}

	override fun findByUserId(projectId: UUID, userId: UUID): Flux<ParticipantModel> {
		return repository.findByUserId(projectId, userId, null).map(mapper::toModel)
	}

	override fun findAllByUserId(userId: UUID): Flux<ParticipantModel> {
		return repository.findAllByUserId(userId).map(mapper::toModel)
	}

	override fun findWithLimit(
		limit: Int,
		projectId: UUID,
		searchParams: ParticipantSearchParamModel
	): Flux<ParticipantModel> {
		return repository.findWithLimit(
			projectId,
			searchParams.textSearched,
			searchParams.isMajor,
			searchParams.typeSearched,
			searchParams.visibilitySearched,
			searchParams.availabilitySearched,
			searchParams.presenceSearched,
			searchParams.dateTimeSearched,
			limit,
		).map(mapper::toModel)
	}

	override fun updateAllEndAvailability(
		ids: List<UUID>,
		endAvailability: CustomDateTimeModel
	): Flux<ParticipantModel> {
		return if (ids.isEmpty()) Flux.empty()
		else repository.updateAllEndAvailability(ids, endAvailability.time, endAvailability.date).map(mapper::toModel)
	}

	override fun saveAllGuest(guests: List<ParticipantModel>): Flux<ParticipantModel> {
		return if (guests.isEmpty()) Flux.empty()
		else repository.saveAll(guests.map(mapper::toEntity)).map(mapper::toModel)
	}

	override fun deleteAll(ids: List<UUID>): Mono<Unit> {
		return if (ids.isEmpty()) Mono.empty()
		else repository.deleteAllById(ids).thenReturn(Unit)
	}

	override fun findUnusedSince(dateThreshold: LocalDate): Flux<UUID> {
		return repository.findUnusedSince(dateThreshold)
	}

	override fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<ParticipantModel> {
		return repository.findById(projectId, id, visibilitySearched, dateTimeSearched = null).map(mapper::toModel)
	}

	override fun create(element: ParticipantModel): Mono<ParticipantModel> = Mono.from(
		dsl.transactionPublisher { config ->
			val txDsl = config.dsl()
			save(txDsl, element).saveNewGroups(txDsl, element)
		}
	)

	override fun update(element: ParticipantModel): Mono<ParticipantModel> = Mono.from(
		dsl.transactionPublisher { config ->
			val txDsl = config.dsl()
			save(txDsl, element)
				.flatMap { findById(txDsl, element.project!!.id!!, element.id!!) }
				.saveNewGroups(txDsl, element)
				.removeDeletedGroups(txDsl, element)
		}
	)

	private fun findById(txDsl: DSLContext, projectId: UUID, id: UUID): Mono<ParticipantModel> {
		return repository.findById(projectId, id, visibilitySearched = null, dateTimeSearched = null, using = txDsl)
			.map(mapper::toModel)
	}

	fun Mono<ParticipantModel>.saveNewGroups(txDsl: DSLContext, element: ParticipantModel): Mono<ParticipantModel> {
		return flatMap { participant ->
			val newGroups = participant.getNewGroups(element)
			if (newGroups.isEmpty()) return@flatMap Mono.just(participant)
			groupContentRepository.saveAll(newGroups.map { groupContentMapper.toEntity(it.id!!, participant) }, txDsl)
				.map(groupContentMapper::toModel)
				.collectList()
				.map { participant.apply { groups = groups.plus(newGroups) } }
		}
	}

	fun Mono<ParticipantModel>.removeDeletedGroups(
		txDsl: DSLContext,
		element: ParticipantModel
	): Mono<ParticipantModel> {
		return flatMap { participant ->
			val removedGroups = participant.getOldGroupIds(element)
			if (removedGroups.isEmpty()) return@flatMap Mono.just(participant)
			groupContentRepository.deleteAllByParticipantIdAndGroupIds(participant.id!!, removedGroups, txDsl)
				.then(Mono.fromCallable {
					participant.apply {
						groups = groups.filter { removedGroups.contains(it.id) }
					}
				})
		}
	}

	private fun save(txDsl: DSLContext, element: ParticipantModel): Mono<ParticipantModel> {
		return repository.save(mapper.toEntity(element), txDsl).map(mapper::toModel)
	}

	override fun deleteById(id: UUID): Mono<Unit> {
		return repository.deleteById(id).thenReturn(Unit)
	}
}
