package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.routines.references.unaccent2
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbProject
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbUser
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_GROUP
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_GROUP_CONTENT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_MOVEMENT_CONTENT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_PARTICIPANT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.GenericColumns
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.activeAtCondition
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.activeNowCondition
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.creatorTable
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.editorTable
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.fillGeneric
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.fillGenericProject
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.projectTable
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.setGeneric
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.setGenericProject
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.visibleCondition
import org.jooq.CommonTableExpression
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.JSON
import org.jooq.Record
import org.jooq.SelectField
import org.jooq.SelectOnConditionStep
import org.jooq.Table
import org.jooq.impl.DSL
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.jsonArrayAgg
import org.jooq.impl.DSL.max
import org.jooq.impl.DSL.name
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.util.UUID

@Repository
class GroupJooqRepository(private val dsl: DSLContext) {
	private val columns = GenericColumns(
		TB_GROUP.ID,
		TB_GROUP.VISIBLE,
		TB_GROUP.CREATED_DATE,
		TB_GROUP.CREATED_BY,
		TB_GROUP.LAST_MODIFIED_DATE,
		TB_GROUP.LAST_MODIFIED_BY,
	)

	private fun participantsGroupsCte(projectId: UUID, using: DSLContext = dsl): CommonTableExpression<*> {
		val groupAlias = TB_GROUP.`as`("grp")
		val groupPresence = TB_GROUP.`as`("group_presence")
		return name("participants_groups").`as`(
			using.select(
				TB_PARTICIPANT.ID, TB_PARTICIPANT.START_AVAILABILITY_DATE, TB_PARTICIPANT.START_AVAILABILITY_TIME,
				TB_PARTICIPANT.END_AVAILABILITY_DATE, TB_PARTICIPANT.END_AVAILABILITY_TIME,
				jsonArrayAgg(groupAlias.ID).filterWhere(groupAlias.ID.isNotNull).`as`("participant_groups"),
				jsonArrayAgg(groupPresence.ID).filterWhere(groupPresence.ID.isNotNull)
					.`as`("participant_groups_available"),
			)
				.from(TB_PARTICIPANT)
				.leftJoin(TB_GROUP_CONTENT).on(TB_GROUP_CONTENT.PARTICIPANT_ID.eq(TB_PARTICIPANT.ID))
				.leftJoin(groupAlias).on(groupAlias.ID.eq(TB_GROUP_CONTENT.GROUP_ID).and(groupAlias.VISIBLE.isTrue))
				.leftJoin(groupPresence).on(
					groupPresence.ID.eq(TB_GROUP_CONTENT.GROUP_ID).and(groupPresence.VISIBLE.isTrue)
						.and(
							activeNowCondition(
								groupPresence.START_AVAILABILITY_DATE,
								groupPresence.START_AVAILABILITY_TIME,
								groupPresence.END_AVAILABILITY_DATE,
								groupPresence.END_AVAILABILITY_TIME
							)
						)
				)
				.where(TB_PARTICIPANT.PROJECT_ID.eq(projectId).and(TB_PARTICIPANT.VISIBLE.isTrue))
				.groupBy(
					TB_PARTICIPANT.ID,
					TB_PARTICIPANT.START_AVAILABILITY_DATE,
					TB_PARTICIPANT.START_AVAILABILITY_TIME,
					TB_PARTICIPANT.END_AVAILABILITY_DATE,
					TB_PARTICIPANT.END_AVAILABILITY_TIME
				)
		)
	}

	private fun insideMembersCte(participantsGroups: Table<*>, using: DSLContext = dsl): CommonTableExpression<*> {
		val plm = using.select(
			max(TB_MOVEMENT.DATE_TIME).`as`("participant_last_movement_date_time"),
			TB_MOVEMENT_CONTENT.PARTICIPANT_ID
		)
			.from(TB_MOVEMENT)
			.join(TB_MOVEMENT_CONTENT).on(TB_MOVEMENT_CONTENT.MOVEMENT_ID.eq(TB_MOVEMENT.ID))
			.where(TB_MOVEMENT.VISIBLE.isTrue)
			.groupBy(TB_MOVEMENT_CONTENT.PARTICIPANT_ID)
			.asTable("plm")
		val plmDateTime = field(name("plm", "participant_last_movement_date_time"), ZonedDateTime::class.java)
		val plmParticipantId = field(name("plm", "participant_id"), UUID::class.java)

		val pgId = field(name("participants_groups", "id"), UUID::class.java)
		val pgStartDate = field(name("participants_groups", "start_availability_date"), LocalDate::class.java)
		val pgStartTime = field(name("participants_groups", "start_availability_time"), OffsetTime::class.java)
		val pgEndDate = field(name("participants_groups", "end_availability_date"), LocalDate::class.java)
		val pgEndTime = field(name("participants_groups", "end_availability_time"), OffsetTime::class.java)
		val pgGroups = field(name("participants_groups", "participant_groups"), JSON::class.java)
		val pgGroupsAvailable = field(name("participants_groups", "participant_groups_available"), JSON::class.java)

		val currentlyInsideCondition = DSL.condition(
			"""
			(
				({0} IS NULL AND ({4} IS NULL OR json_array_length({4}) = 0 OR ({5} IS NOT NULL AND json_array_length({5}) > 0)))
				OR COALESCE({0}, '+infinity'::date) < CURRENT_DATE
				OR (COALESCE({0}, '+infinity'::date) = CURRENT_DATE AND COALESCE({1}, '00:00:00.000000'::time) <= CURRENT_TIME)
			) AND (
				({2} IS NULL AND ({4} IS NULL OR json_array_length({4}) = 0 OR ({5} IS NOT NULL AND json_array_length({5}) > 0)))
				OR COALESCE({2}, '-infinity'::date) > CURRENT_DATE
				OR (COALESCE({2}, '-infinity'::date) = CURRENT_DATE AND COALESCE({3}, '23:59:59.999999'::time) >= CURRENT_TIME)
			)
			""",
			pgStartDate, pgStartTime, pgEndDate, pgEndTime, pgGroups, pgGroupsAvailable,
		)

		return name("inside_members").`as`(
			using.select(TB_GROUP_CONTENT.GROUP_ID, count(TB_MOVEMENT.ID).`as`("inside_members_count"))
				.from(TB_MOVEMENT)
				.join(plm).on(plmDateTime.eq(TB_MOVEMENT.DATE_TIME))
				.join(TB_GROUP_CONTENT).on(plmParticipantId.eq(TB_GROUP_CONTENT.PARTICIPANT_ID))
				.join(participantsGroups).on(plmParticipantId.eq(pgId))
				.where(TB_MOVEMENT.TYPE.eq(MovementTypeEnum.IN).and(currentlyInsideCondition))
				.groupBy(TB_GROUP_CONTENT.GROUP_ID)
		)
	}

	private fun membersCte(using: DSLContext = dsl): CommonTableExpression<*> = name("members").`as`(
		using.select(TB_GROUP_CONTENT.GROUP_ID, count(TB_GROUP_CONTENT.PARTICIPANT_ID).`as`("members_count"))
			.from(TB_GROUP_CONTENT)
			.join(TB_PARTICIPANT).on(TB_GROUP_CONTENT.PARTICIPANT_ID.eq(TB_PARTICIPANT.ID))
			.where(TB_PARTICIPANT.VISIBLE.isTrue)
			.groupBy(TB_GROUP_CONTENT.GROUP_ID)
	)

	private fun searchConditions(
		projectId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		presenceSearched: Boolean?,
		dateTimeSearched: ZonedDateTime?
	): Condition {
		val conditions = mutableListOf(TB_GROUP.PROJECT_ID.eq(projectId))
		textSearched?.let {
			val pattern = DSL.concat(DSL.inline("%"), unaccent2(DSL.`val`(it)), DSL.inline("%"))
			conditions += unaccent2(TB_GROUP.NAME).likeIgnoreCase(pattern)
		}
		conditions += visibleCondition(TB_GROUP.VISIBLE, visibilitySearched)
		presenceSearched?.let {
			val isPresent = activeNowCondition(
				TB_GROUP.START_AVAILABILITY_DATE,
				TB_GROUP.START_AVAILABILITY_TIME,
				TB_GROUP.END_AVAILABILITY_DATE,
				TB_GROUP.END_AVAILABILITY_TIME
			)
			conditions += if (it) isPresent else isPresent.not()
		}
		dateTimeSearched?.let { conditions += dateInRangeCondition(it) }
		return DSL.and(conditions)
	}

	private fun dateInRangeCondition(dateTimeSearched: ZonedDateTime): Condition =
		activeAtCondition(
			TB_GROUP.START_AVAILABILITY_DATE,
			TB_GROUP.START_AVAILABILITY_TIME,
			TB_GROUP.END_AVAILABILITY_DATE,
			TB_GROUP.END_AVAILABILITY_TIME,
			dateTimeSearched
		)

	private val membersCount: Field<Long> = field(name("members", "members_count"), Long::class.java)
	private val insideMembersCountRaw: Field<Long> =
		field(name("inside_members", "inside_members_count"), Long::class.java)
	private val fullMembersCount: Field<Long> = field(name("full_members_count"), Long::class.java)
	private val fullInsideMembersCount: Field<Long> = field(name("full_inside_members_count"), Long::class.java)
	private val fullOutsideMembersCount: Field<Long> = field(name("full_outside_members_count"), Long::class.java)

	private fun baseSelect(
		projectId: UUID,
		project: TbProject,
		creator: TbUser,
		editor: TbUser,
		vararg extraFields: SelectField<*>,
		using: DSLContext = dsl,
	): SelectOnConditionStep<Record> {
		val participantsGroups = participantsGroupsCte(projectId, using)
		val insideMembers = insideMembersCte(participantsGroups, using)
		val members = membersCte(using)
		return using.with(participantsGroups, insideMembers, members)
			.select(
				listOf(
					TB_GROUP.asterisk(),
					membersCount.`as`("full_members_count"),
					DSL.coalesce(insideMembersCountRaw, DSL.inline(0L)).`as`("full_inside_members_count"),
					membersCount.minus(DSL.coalesce(insideMembersCountRaw, DSL.inline(0L)))
						.`as`("full_outside_members_count"),
					project.NAME,
					project.BEGIN_DATE,
					project.BEGIN_TIME,
					project.END_DATE,
					project.END_TIME,
					project.OPTIONS,
					creator.FIRST_NAME,
					creator.LAST_NAME,
					creator.EMAIL,
					editor.FIRST_NAME,
					editor.LAST_NAME,
					editor.EMAIL,
				) + extraFields.toList()
			)
			.from(TB_GROUP)
			.leftJoin(insideMembers).on(field(name("inside_members", "group_id"), UUID::class.java).eq(TB_GROUP.ID))
			.join(members).on(field(name("members", "group_id"), UUID::class.java).eq(TB_GROUP.ID))
			.join(project).on(TB_GROUP.PROJECT_ID.eq(project.ID).and(project.VISIBLE.isTrue))
			.leftJoin(creator).on(TB_GROUP.CREATED_BY.eq(creator.ID))
			.leftJoin(editor).on(TB_GROUP.LAST_MODIFIED_BY.eq(editor.ID))
	}

	fun findAll(
		projectId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		presenceSearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
		limit: Int,
		offset: Int,
	): Flux<GroupEntity> {
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		val fullCount = count().over().`as`("full_count")
		return Flux.from(
			baseSelect(projectId, project, creator, editor, fullCount)
				.where(
					searchConditions(
						projectId,
						textSearched,
						visibilitySearched,
						presenceSearched,
						dateTimeSearched
					)
				)
				.orderBy(TB_GROUP.NAME)
				.limit(limit).offset(offset)
		).map { it.toEntity(project, creator, editor, fullCount, includeCounts = true) }
	}

	fun findAllByIds(projectId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<GroupEntity> {
		if (ids.isEmpty()) return Flux.empty()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		return Flux.from(
			baseSelect(projectId, project, creator, editor)
				.where(
					TB_GROUP.PROJECT_ID.eq(projectId).and(TB_GROUP.ID.`in`(ids))
						.and(visibleCondition(TB_GROUP.VISIBLE, visibilitySearched))
				)
		).map { it.toEntity(project, creator, editor, includeCounts = true) }
	}

	fun findWithLimit(
		projectId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		presenceSearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
		limit: Int,
	): Flux<GroupEntity> {
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		return Flux.from(
			baseSelect(projectId, project, creator, editor)
				.where(
					searchConditions(
						projectId,
						textSearched,
						visibilitySearched,
						presenceSearched,
						dateTimeSearched
					)
				)
				.orderBy(TB_GROUP.NAME)
				.limit(limit)
		).map { it.toEntity(project, creator, editor, includeCounts = true) }
	}

	fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<GroupEntity> =
		findById(projectId, id, visibilitySearched, dsl)

	fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?, using: DSLContext): Mono<GroupEntity> {
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		return Mono.from(
			baseSelect(projectId, project, creator, editor, using = using)
				.where(
					TB_GROUP.PROJECT_ID.eq(projectId).and(TB_GROUP.ID.eq(id))
						.and(visibleCondition(TB_GROUP.VISIBLE, visibilitySearched))
				)
		).map { it.toEntity(project, creator, editor, includeCounts = true) }
	}

	fun findEmpty(participantToExclude: List<UUID>): Flux<UUID> {
		val gc = dsl.select(TB_GROUP_CONTENT.GROUP_ID, count(TB_GROUP_CONTENT.ID).`as`("count"))
			.from(TB_GROUP_CONTENT)
			.where(TB_GROUP_CONTENT.PARTICIPANT_ID.notIn(participantToExclude))
			.groupBy(TB_GROUP_CONTENT.GROUP_ID)
			.asTable("gc")
		val gcGroupId = field(name("gc", "group_id"), UUID::class.java)
		val gcCount = field(name("gc", "count"), Long::class.java)
		return Flux.from(
			dsl.select(TB_GROUP.ID)
				.from(TB_GROUP)
				.leftJoin(gc).on(TB_GROUP.ID.eq(gcGroupId))
				.where(gcCount.isNull.or(gcCount.eq(0L)))
		).map { it.value1()!! }
	}

	fun save(entity: GroupEntity): Mono<GroupEntity> = save(entity, dsl)

	fun save(entity: GroupEntity, using: DSLContext): Mono<GroupEntity> =
		if (entity.id == null) insert(using, entity) else update(using, entity)

	private fun insert(using: DSLContext, entity: GroupEntity): Mono<GroupEntity> {
		val record = using.newRecord(TB_GROUP)
		record.setGeneric(entity, columns)
		record.setGenericProject(entity, TB_GROUP.PROJECT_ID)
		entity.name?.let { record.set(TB_GROUP.NAME, it) }
		entity.startAvailabilityDate?.let { record.set(TB_GROUP.START_AVAILABILITY_DATE, it) }
		entity.startAvailabilityTime?.let { record.set(TB_GROUP.START_AVAILABILITY_TIME, it) }
		entity.endAvailabilityDate?.let { record.set(TB_GROUP.END_AVAILABILITY_DATE, it) }
		entity.endAvailabilityTime?.let { record.set(TB_GROUP.END_AVAILABILITY_TIME, it) }
		return Mono.from(using.insertInto(TB_GROUP).set(record).returning()).map { it.toEntity() }
	}

	private fun update(using: DSLContext, entity: GroupEntity): Mono<GroupEntity> = Mono.from(
		using.update(TB_GROUP)
			.setGeneric(entity, columns)
			.setGenericProject(entity, TB_GROUP.PROJECT_ID)
			.set(TB_GROUP.NAME, entity.name)
			.set(TB_GROUP.START_AVAILABILITY_DATE, entity.startAvailabilityDate)
			.set(TB_GROUP.START_AVAILABILITY_TIME, entity.startAvailabilityTime)
			.set(TB_GROUP.END_AVAILABILITY_DATE, entity.endAvailabilityDate)
			.set(TB_GROUP.END_AVAILABILITY_TIME, entity.endAvailabilityTime)
			.where(TB_GROUP.ID.eq(entity.id))
			.returning()
	).map { it.toEntity() }

	fun deleteById(id: UUID): Mono<Unit> = Mono.from(dsl.deleteFrom(TB_GROUP).where(TB_GROUP.ID.eq(id))).map { }

	private fun Record.toEntity(
		project: TbProject? = null,
		creator: TbUser? = null,
		editor: TbUser? = null,
		fullCount: Field<Int>? = null,
		includeCounts: Boolean = false,
	): GroupEntity = GroupEntity(
		name = get(TB_GROUP.NAME),
		startAvailabilityDate = get(TB_GROUP.START_AVAILABILITY_DATE),
		startAvailabilityTime = get(TB_GROUP.START_AVAILABILITY_TIME),
		endAvailabilityDate = get(TB_GROUP.END_AVAILABILITY_DATE),
		endAvailabilityTime = get(TB_GROUP.END_AVAILABILITY_TIME),
		members = if (includeCounts) get(fullMembersCount) else null,
		insideMembers = if (includeCounts) get(fullInsideMembersCount) else null,
		outsideMembers = if (includeCounts) get(fullOutsideMembersCount) else null,
	).apply {
		fillGeneric(this, columns, creator, editor, fullCount)
		fillGenericProject(this, TB_GROUP.PROJECT_ID, project)
	}
}
