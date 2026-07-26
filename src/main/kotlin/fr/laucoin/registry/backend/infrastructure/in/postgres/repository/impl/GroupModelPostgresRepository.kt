package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.GroupSortFieldEnum
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupQueries.DATE_IN_GROUP_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupQueries.GROUP_INSIDE_MEMBERS_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupQueries.GROUP_MEMBERS_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupQueries.GROUP_PRESENCE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupQueries.GROUP_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupQueries.SELECT_MEMBERS_COUNTS
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupQueries.WITH_GROUP_INSIDE_MEMBERS
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupQueries.WITH_GROUP_MEMBERS
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupQueries.WITH_PARTICIPANT_GROUPS
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.GroupContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.GroupEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.PROJECT_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.PROJECT_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_LINKED_PROJECT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.VISIBLE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.IGroupContentEntityRepository
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.IGroupEntityRepository
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.UUID

@Service
class GroupModelPostgresRepository(
	private val repository: IGroupEntityRepository,
	private val contentRepository: IGroupContentEntityRepository,
	private val transactionalOperator: TransactionalOperator,
	private val mapper: GroupEntityMapper,
	private val contentMapper: GroupContentEntityMapper,
	private val databaseClient: DatabaseClient,
	private val converter: MappingR2dbcConverter,
) : IGroupPort {
	override fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: GroupSearchParamModel,
		sort: List<SortModel<GroupSortFieldEnum>>,
	): Mono<PageModel<GroupModel>> {
		val entities = if (sort.isEmpty()) {
			repository.findAll(
				projectId,
				searchParams.textSearched,
				searchParams.visibilitySearched,
				searchParams.presenceSearched,
				searchParams.dateTimeSearched,
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
				searchParams.visibilitySearched,
				searchParams.presenceSearched,
				searchParams.dateTimeSearched,
			),
			entities.map(mapper::toModel).collectList(),
		).map {
			PageModel(pageable, it.t1, it.t2)
		}
	}

	/**
	 * API v2 sorted page (ADR 017 §5). The ORDER BY is built exclusively from
	 * the [GroupSortFieldEnum] whitelist ([toColumn]) — user input never
	 * reaches the SQL string. Row mapping reuses the same converter Spring Data
	 * applies to the annotated queries.
	 */
	private fun findAllSorted(
		projectId: UUID,
		searchParams: GroupSearchParamModel,
		pageable: PageableModel,
		sort: List<SortModel<GroupSortFieldEnum>>,
	): Flux<GroupEntity> {
		val orderBy =
			sort.joinToString(", ") { "t.${it.field.toColumn()} ${if (it.descending) "DESC" else "ASC"}" } + ", t.$ID ASC"
		val sql = """
        WITH $WITH_PARTICIPANT_GROUPS, $WITH_GROUP_INSIDE_MEMBERS, $WITH_GROUP_MEMBERS
        SELECT t.*, $SELECT_MEMBERS_COUNTS, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $GROUP_TABLE t $GROUP_INSIDE_MEMBERS_JOIN $GROUP_MEMBERS_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND $GROUP_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $GROUP_PRESENCE_CLAUSE AND $DATE_IN_GROUP_DATES_RANGE_CLAUSE
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
		spec = searchParams.visibilitySearched
			?.let { spec.bind("visibilitySearched", it) }
			?: spec.bindNull("visibilitySearched", Boolean::class.javaObjectType)
		spec = searchParams.presenceSearched
			?.let { spec.bind("presenceSearched", it) }
			?: spec.bindNull("presenceSearched", Boolean::class.javaObjectType)
		spec = searchParams.dateTimeSearched
			?.let { spec.bind("dateTimeSearched", it.toOffsetDateTime()) }
			?: spec.bindNull("dateTimeSearched", OffsetDateTime::class.java)

		return spec
			.map { row, metadata -> converter.read(GroupEntity::class.java, row, metadata) }
			.all()
	}

	private fun GroupSortFieldEnum.toColumn(): String = when (this) {
		GroupSortFieldEnum.NAME -> GROUP_NAME
		GroupSortFieldEnum.START_AVAILABILITY_DATE -> GROUP_START_AVAILABILITY_DATE
		GroupSortFieldEnum.END_AVAILABILITY_DATE -> GROUP_END_AVAILABILITY_DATE
	}

	override fun findContent(
		projectId: UUID,
		groupIds: List<UUID>,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
	): Flux<Pair<UUID, List<ParticipantModel>>> {
		return if (groupIds.isEmpty()) Flux.empty()
		else contentRepository.findAllByGroupIds(projectId, groupIds, visibilitySearched, availabilitySearched)
			.groupBy { it.groupId!! }
			.flatMap {
				it.collectList().map { list -> it.key() to list.map(contentMapper::toModel) }
			}
	}

	override fun findAllByIds(projectId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<GroupModel> {
		return if (ids.isEmpty()) Flux.empty() else repository.findAllByIds(projectId, ids, visibilitySearched)
			.map(mapper::toModel)
	}

	override fun findWithLimit(limit: Int, projectId: UUID, searchParams: GroupSearchParamModel): Flux<GroupModel> {
		return repository.findWithLimit(
			projectId,
			searchParams.textSearched,
			searchParams.visibilitySearched,
			searchParams.presenceSearched,
			searchParams.dateTimeSearched,
			limit,
		).map(mapper::toModel)
	}

	override fun findEmpty(participantToExclude: List<UUID>): Flux<UUID> {
		return if (participantToExclude.isEmpty()) Flux.empty()
		else repository.findEmpty(participantToExclude)
	}

	override fun findByIdWithContent(
		projectId: UUID,
		id: UUID,
		visibilitySearched: Boolean?,
		memberVisibilitySearched: Boolean?,
		memberAvailabilitySearched: Boolean?,
	): Mono<GroupModel> {
		return Mono.zip(
			repository.findById(projectId, id, visibilitySearched).map(mapper::toModel),
			findContent(projectId, listOf(id), memberVisibilitySearched, memberAvailabilitySearched).collectList()
				.handle<List<ParticipantModel>> { it, handle ->
					if (it.isEmpty()) handle.next(emptyList()) else handle.next(
						it.first().second
					)
				}
		).map {
			it.t1.members = it.t2
			it.t1
		}
	}

	override fun create(element: GroupModel): Mono<GroupModel> {
		return save(element)
			.saveNewMembers(element)
			.`as`(transactionalOperator::transactional)
	}

	override fun update(element: GroupModel): Mono<GroupModel> {
		return save(element)
			.flatMap {
				findByIdWithContent(
					element.project!!.id!!,
					element.id!!,
					visibilitySearched = null,
					memberVisibilitySearched = null,
					memberAvailabilitySearched = null,
				)
			}
			.removeDeletedMembers(element)
			.saveNewMembers(element)
			.`as`(transactionalOperator::transactional)
	}

	fun Mono<GroupModel>.saveNewMembers(element: GroupModel): Mono<GroupModel> {
		return flatMap { group ->
			val newMembers = group.getNewMembers(element)
			if (newMembers.isEmpty()) return@flatMap Mono.just(group)
			contentRepository.saveAll(newMembers.map { contentMapper.toEntity(group.id!!, it) })
				.map(contentMapper::toModel)
				.collectList()
				.map { group.apply { members = members.plus(it) } }
		}
	}

	fun Mono<GroupModel>.removeDeletedMembers(element: GroupModel): Mono<GroupModel> {
		return flatMap { group ->
			val removedMembers = group.getOldMemberIds(element)
			if (removedMembers.isEmpty()) return@flatMap Mono.just(group)
			contentRepository.deleteAllByGroupIdAndParticipantIds(group.id!!, removedMembers)
				.then(Mono.fromCallable {
					group.apply {
						members = members.filter { !removedMembers.contains(it.id) }
					}
				})
		}
	}

	private fun save(element: GroupModel): Mono<GroupModel> {
		return repository.save(mapper.toEntity(element)).map(mapper::toModel)
	}

	override fun deleteById(id: UUID): Mono<Unit> {
		return repository.deleteById(id).thenReturn(Unit)
	}
}
