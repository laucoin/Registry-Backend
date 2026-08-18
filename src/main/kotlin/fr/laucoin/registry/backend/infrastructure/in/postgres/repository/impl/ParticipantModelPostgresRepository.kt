package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.ParticipantSortFieldEnum
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_BIRTHDAY
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_TYPE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.GROUPS_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.LAST_MOVEMENT_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.PARTICIPANT_AVAILABILITY_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.PARTICIPANT_DEPARTED_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.PARTICIPANT_GROUPED_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.PARTICIPANT_MAJOR_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.PARTICIPANT_STATUS_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.PARTICIPANT_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.PARTICIPANT_TYPE_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.PARTICIPANT_WARNED_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.SELECT_LAST_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.SELECT_LINKED_GROUPS
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.SELECT_LINKED_USER
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.SELECT_PARTICIPANT_SEARCH
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.USER_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.WITH_PARTICIPANT_GROUPS
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.WITH_PARTICIPANT_LAST_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.GroupContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.ParticipantEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.PROJECT_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.PROJECT_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_LINKED_PROJECT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.VISIBLE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.orderByWithRelevance
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.IGroupContentEntityRepository
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.IParticipantEntityRepository
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.UUID

@Service
class ParticipantModelPostgresRepository(
	private val repository: IParticipantEntityRepository,
	private val groupContentRepository: IGroupContentEntityRepository,
	private val transactionalOperator: TransactionalOperator,
	private val mapper: ParticipantEntityMapper,
	private val groupContentMapper: GroupContentEntityMapper,
	private val databaseClient: DatabaseClient,
	private val converter: MappingR2dbcConverter,
) : IParticipantPort {
	override fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ParticipantSearchParamModel,
		sort: List<SortModel<ParticipantSortFieldEnum>>,
	): Mono<PageModel<ParticipantModel>> {
		val entities = if (sort.isEmpty()) {
			repository.findAll(
				projectId,
				searchParams.textSearched,
				searchParams.isMajor,
				searchParams.typeSearched,
				searchParams.visibilitySearched,
				searchParams.availabilitySearched,
				searchParams.presenceStatusSearched?.name,
				searchParams.departedSearched,
				searchParams.warnedSearched,
				searchParams.dateTimeSearched,
				searchParams.groupedSearched,
				pageable.limit,
				pageable.offset,
			)
		} else {
			findAllSorted(projectId, searchParams, pageable, sort)
		}

		return Mono.zip(
			repository.countAll(
				projectId,
				searchParams.textSearched,
				searchParams.isMajor,
				searchParams.typeSearched,
				searchParams.visibilitySearched,
				searchParams.availabilitySearched,
				searchParams.presenceStatusSearched?.name,
				searchParams.departedSearched,
				searchParams.warnedSearched,
				searchParams.dateTimeSearched,
				searchParams.groupedSearched,
			),
			entities.map(mapper::toModel).collectList()
		).map {
			PageModel(pageable, it.t1, it.t2)
		}
	}

	/**
	 * API v2 sorted page. The ORDER BY is built exclusively from
	 * the [ParticipantSortFieldEnum] whitelist ([toColumn]) — user input never
	 * reaches the SQL string. Row mapping reuses the same converter Spring Data
	 * applies to the annotated queries.
	 */
	private fun findAllSorted(
		projectId: UUID,
		searchParams: ParticipantSearchParamModel,
		pageable: PageableModel,
		sort: List<SortModel<ParticipantSortFieldEnum>>,
	): Flux<ParticipantEntity> {
		val orderBy = orderByWithRelevance(
			searchParams.textSearched,
			sort.joinToString(", ") { "t.${it.field.toColumn()} ${if (it.descending) "DESC" else "ASC"}" },
		)
		val sql = """
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LAST_MOVEMENT, $SELECT_LINKED_GROUPS, $SELECT_PARTICIPANT_SEARCH, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $LAST_MOVEMENT_JOIN $GROUPS_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND $PARTICIPANT_TEXT_SEARCH_CLAUSE AND $PARTICIPANT_MAJOR_CLAUSE AND $PARTICIPANT_TYPE_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $PARTICIPANT_AVAILABILITY_CLAUSE AND $PARTICIPANT_STATUS_CLAUSE AND $PARTICIPANT_DEPARTED_CLAUSE AND $PARTICIPANT_WARNED_CLAUSE AND $DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE AND $PARTICIPANT_GROUPED_CLAUSE
        ORDER BY $orderBy
        LIMIT :limit OFFSET :offset
        """

		var spec = databaseClient.sql(sql)
			.bind("projectId", projectId)
			.bind("limit", pageable.limit)
			.bind("offset", pageable.offset)
		spec = searchParams.textSearched
			?.let { spec.bind("textSearched", it) }
			?: spec.bindNull("textSearched", String::class.java)
		spec = searchParams.isMajor
			?.let { spec.bind("isMajor", it) }
			?: spec.bindNull("isMajor", Boolean::class.javaObjectType)
		spec = searchParams.typeSearched
			?.let { spec.bind("typeSearched", it.name) }
			?: spec.bindNull("typeSearched", String::class.java)
		spec = searchParams.visibilitySearched
			?.let { spec.bind("visibilitySearched", it) }
			?: spec.bindNull("visibilitySearched", Boolean::class.javaObjectType)
		spec = searchParams.availabilitySearched
			?.let { spec.bind("availabilitySearched", it) }
			?: spec.bindNull("availabilitySearched", Boolean::class.javaObjectType)
		spec = searchParams.presenceStatusSearched
			?.let { spec.bind("statusSearched", it.name) }
			?: spec.bindNull("statusSearched", String::class.java)
		spec = searchParams.departedSearched
			?.let { spec.bind("departedSearched", it) }
			?: spec.bindNull("departedSearched", Boolean::class.javaObjectType)
		spec = searchParams.warnedSearched
			?.let { spec.bind("warnedSearched", it) }
			?: spec.bindNull("warnedSearched", Boolean::class.javaObjectType)
		spec = searchParams.groupedSearched
			?.let { spec.bind("groupedSearched", it) }
			?: spec.bindNull("groupedSearched", Boolean::class.javaObjectType)
		spec = searchParams.dateTimeSearched
			?.let { spec.bind("dateTimeSearched", it.toOffsetDateTime()) }
			?: spec.bindNull("dateTimeSearched", OffsetDateTime::class.java)

		return spec
			.map { row, metadata -> converter.read(ParticipantEntity::class.java, row, metadata) }
			.all()
	}

	private fun ParticipantSortFieldEnum.toColumn(): String = when (this) {
		ParticipantSortFieldEnum.FIRST_NAME -> PARTICIPANT_FIRST_NAME
		ParticipantSortFieldEnum.LAST_NAME -> PARTICIPANT_LAST_NAME
		ParticipantSortFieldEnum.BIRTHDAY -> PARTICIPANT_BIRTHDAY
		ParticipantSortFieldEnum.TYPE -> PARTICIPANT_TYPE
	}

	override fun findBirthdays(projectId: UUID, visibilitySearched: Boolean?, limit: Int): Flux<ParticipantModel> {
		return repository.findAllWithBirthday(projectId, visibilitySearched, limit)
			.map(mapper::toModel)
	}

	override fun findArrivingToday(projectId: UUID, visibilitySearched: Boolean?, limit: Int): Flux<ParticipantModel> {
		return repository.findArrivingToday(projectId, visibilitySearched, limit)
			.map(mapper::toModel)
	}

	override fun findDepartingToday(projectId: UUID, visibilitySearched: Boolean?, limit: Int): Flux<ParticipantModel> {
		return repository.findDepartingToday(projectId, visibilitySearched, limit)
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
			searchParams.presenceStatusSearched?.name,
			searchParams.departedSearched,
			searchParams.warnedSearched,
			searchParams.dateTimeSearched,
			searchParams.groupedSearched,
		)
	}

	override fun findPageByGroupId(
		projectId: UUID,
		groupId: UUID,
		pageable: PageableModel,
		searchParams: ParticipantSearchParamModel
	): Mono<PageModel<ParticipantModel>> {
		return Mono.zip(
			repository.countAllByGroupId(
				projectId,
				groupId,
				searchParams.textSearched,
				searchParams.isMajor,
				searchParams.typeSearched,
				searchParams.visibilitySearched,
				searchParams.availabilitySearched,
				searchParams.presenceStatusSearched?.name,
				searchParams.departedSearched,
				searchParams.warnedSearched,
				searchParams.dateTimeSearched,
			),
			repository.findAllByGroupId(
				projectId,
				groupId,
				searchParams.textSearched,
				searchParams.isMajor,
				searchParams.typeSearched,
				searchParams.visibilitySearched,
				searchParams.availabilitySearched,
				searchParams.presenceStatusSearched?.name,
				searchParams.departedSearched,
				searchParams.warnedSearched,
				searchParams.dateTimeSearched,
				pageable.limit,
				pageable.offset,
			).map(mapper::toModel).collectList()
		).map {
			PageModel(pageable, it.t1, it.t2)
		}
	}

	override fun findAllByIds(projectId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<ParticipantModel> {
		return if (ids.isEmpty()) Flux.empty()
		else repository.findAllByIds(projectId, ids, visibilitySearched, dateTimeSearched = null).map(mapper::toModel)
	}

	override fun findByUserId(projectId: UUID, userId: UUID): Flux<ParticipantModel> {
		return repository.findByUserId(projectId, userId, null).map(mapper::toModel)
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
			searchParams.presenceStatusSearched?.name,
			searchParams.departedSearched,
			searchParams.warnedSearched,
			searchParams.dateTimeSearched,
			limit,
		).map(mapper::toModel)
	}

	override fun markAllAsDeparted(ids: List<UUID>, departedAt: ZonedDateTime): Flux<ParticipantModel> {
		return if (ids.isEmpty()) Flux.empty()
		else repository.markAllAsDeparted(ids, departedAt).map(mapper::toModel)
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

	override fun create(element: ParticipantModel): Mono<ParticipantModel> {
		return save(element)
			.saveNewGroups(element)
			.`as`(transactionalOperator::transactional)
	}

	override fun update(element: ParticipantModel): Mono<ParticipantModel> {
		return save(element)
			.flatMap { findById(element.project!!.id!!, element.id!!, visibilitySearched = null) }
			.saveNewGroups(element)
			.removeDeletedGroups(element)
			.`as`(transactionalOperator::transactional)
	}

	fun Mono<ParticipantModel>.saveNewGroups(element: ParticipantModel): Mono<ParticipantModel> {
		return flatMap { participant ->
			val newGroups = participant.getNewGroups(element)
			if (newGroups.isEmpty()) return@flatMap Mono.just(participant)
			groupContentRepository.saveAll(newGroups.map { groupContentMapper.toEntity(it.id!!, participant) })
				.map(groupContentMapper::toModel)
				.collectList()
				.map { participant.apply { groups = groups.plus(newGroups) } }
		}
	}

	fun Mono<ParticipantModel>.removeDeletedGroups(element: ParticipantModel): Mono<ParticipantModel> {
		return flatMap { participant ->
			val removedGroups = participant.getOldGroupIds(element)
			if (removedGroups.isEmpty()) return@flatMap Mono.just(participant)
			groupContentRepository.deleteAllByParticipantIdAndGroupIds(participant.id!!, removedGroups)
				.then(Mono.fromCallable {
					participant.apply {
						groups = groups.filter { removedGroups.contains(it.id) }
					}
				})
		}
	}

	private fun save(element: ParticipantModel): Mono<ParticipantModel> {
		return repository.save(mapper.toEntity(element)).map(mapper::toModel)
	}

	override fun deleteById(id: UUID): Mono<Unit> {
		return repository.deleteById(id).thenReturn(Unit)
	}
}
