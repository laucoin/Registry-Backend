package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbProject
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbUser
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_GROUP
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_GROUP_CONTENT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_MOVEMENT_CONTENT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_PARTICIPANT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.routines.references.unaccent2
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.creatorTable
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.editorTable
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.projectTable
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.visibleCondition
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.util.UUID
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

@Repository
class GroupJooqRepository(private val dsl: DSLContext) {
	private fun currentlyUsableCondition(startDate: Field<LocalDate?>, startTime: Field<OffsetTime?>, endDate: Field<LocalDate?>, endTime: Field<OffsetTime?>): Condition {
		val started = startDate.isNull.or(startDate.lt(DSL.currentLocalDate())).or(startDate.eq(DSL.currentLocalDate()).and(startTime.isNull.or(startTime.le(DSL.currentOffsetTime()))))
		val notEnded = endDate.isNull.or(endDate.gt(DSL.currentLocalDate())).or(endDate.eq(DSL.currentLocalDate()).and(endTime.isNull.or(endTime.ge(DSL.currentOffsetTime()))))
		return started.and(notEnded)
	}

	// Mirrors GroupQueries.WITH_PARTICIPANT_GROUPS: for each participant in the project, the visible
	// groups they belong to, and which of those groups are *currently* available for them.
	private fun participantsGroupsCte(projectId: UUID): CommonTableExpression<*> {
		val groupAlias = TB_GROUP.`as`("grp")
		val groupPresence = TB_GROUP.`as`("group_presence")
		return name("participants_groups").`as`(
			dsl.select(
				TB_PARTICIPANT.ID, TB_PARTICIPANT.START_AVAILABILITY_DATE, TB_PARTICIPANT.START_AVAILABILITY_TIME,
				TB_PARTICIPANT.END_AVAILABILITY_DATE, TB_PARTICIPANT.END_AVAILABILITY_TIME,
				jsonArrayAgg(groupAlias.ID).filterWhere(groupAlias.ID.isNotNull).`as`("participant_groups"),
				jsonArrayAgg(groupPresence.ID).filterWhere(groupPresence.ID.isNotNull).`as`("participant_groups_available"),
			)
				.from(TB_PARTICIPANT)
				.leftJoin(TB_GROUP_CONTENT).on(TB_GROUP_CONTENT.PARTICIPANT_ID.eq(TB_PARTICIPANT.ID))
				.leftJoin(groupAlias).on(groupAlias.ID.eq(TB_GROUP_CONTENT.GROUP_ID).and(groupAlias.VISIBLE.isTrue))
				.leftJoin(groupPresence).on(
					groupPresence.ID.eq(TB_GROUP_CONTENT.GROUP_ID).and(groupPresence.VISIBLE.isTrue)
						.and(currentlyUsableCondition(groupPresence.START_AVAILABILITY_DATE, groupPresence.START_AVAILABILITY_TIME, groupPresence.END_AVAILABILITY_DATE, groupPresence.END_AVAILABILITY_TIME))
				)
				.where(TB_PARTICIPANT.PROJECT_ID.eq(projectId).and(TB_PARTICIPANT.VISIBLE.isTrue))
				.groupBy(TB_PARTICIPANT.ID, TB_PARTICIPANT.START_AVAILABILITY_DATE, TB_PARTICIPANT.START_AVAILABILITY_TIME, TB_PARTICIPANT.END_AVAILABILITY_DATE, TB_PARTICIPANT.END_AVAILABILITY_TIME)
		)
	}

	// Mirrors GroupQueries.WITH_GROUP_INSIDE_MEMBERS: per group, how many members' most recent
	// movement was IN and they're currently available (own window, or via an available group override).
	private fun insideMembersCte(participantsGroups: Table<*>): CommonTableExpression<*> {
		val plm = dsl.select(max(TB_MOVEMENT.DATE_TIME).`as`("participant_last_movement_date_time"), TB_MOVEMENT_CONTENT.PARTICIPANT_ID)
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
			dsl.select(TB_GROUP_CONTENT.GROUP_ID, count(TB_MOVEMENT.ID).`as`("inside_members_count"))
				.from(TB_MOVEMENT)
				.join(plm).on(plmDateTime.eq(TB_MOVEMENT.DATE_TIME))
				.join(TB_GROUP_CONTENT).on(plmParticipantId.eq(TB_GROUP_CONTENT.PARTICIPANT_ID))
				.join(participantsGroups).on(plmParticipantId.eq(pgId))
				.where(TB_MOVEMENT.TYPE.eq(MovementTypeEnum.IN).and(currentlyInsideCondition))
				.groupBy(TB_GROUP_CONTENT.GROUP_ID)
		)
	}

	private fun membersCte(): CommonTableExpression<*> = name("members").`as`(
		dsl.select(TB_GROUP_CONTENT.GROUP_ID, count(TB_GROUP_CONTENT.PARTICIPANT_ID).`as`("members_count"))
			.from(TB_GROUP_CONTENT)
			.join(TB_PARTICIPANT).on(TB_GROUP_CONTENT.PARTICIPANT_ID.eq(TB_PARTICIPANT.ID))
			.where(TB_PARTICIPANT.VISIBLE.isTrue)
			.groupBy(TB_GROUP_CONTENT.GROUP_ID)
	)

	private fun searchConditions(projectId: UUID, textSearched: String?, visibilitySearched: Boolean?, presenceSearched: Boolean?, dateTimeSearched: ZonedDateTime?): Condition {
		val conditions = mutableListOf(TB_GROUP.PROJECT_ID.eq(projectId))
		textSearched?.let {
			val pattern = DSL.concat(DSL.inline("%"), unaccent2(DSL.`val`(it)), DSL.inline("%"))
			conditions += unaccent2(TB_GROUP.NAME).likeIgnoreCase(pattern)
		}
		conditions += visibleCondition(TB_GROUP.VISIBLE, visibilitySearched)
		presenceSearched?.let {
			val isPresent = currentlyUsableCondition(TB_GROUP.START_AVAILABILITY_DATE, TB_GROUP.START_AVAILABILITY_TIME, TB_GROUP.END_AVAILABILITY_DATE, TB_GROUP.END_AVAILABILITY_TIME)
			conditions += if (it) isPresent else isPresent.not()
		}
		dateTimeSearched?.let { conditions += dateInRangeCondition(it) }
		return DSL.and(conditions)
	}

	private fun dateInRangeCondition(dateTimeSearched: ZonedDateTime): Condition {
		val date = dateTimeSearched.toLocalDate()
		val time = dateTimeSearched.toOffsetDateTime().toOffsetTime()
		val startsBefore = TB_GROUP.START_AVAILABILITY_DATE.isNull
			.or(TB_GROUP.START_AVAILABILITY_DATE.lt(date))
			.or(TB_GROUP.START_AVAILABILITY_DATE.eq(date).and(TB_GROUP.START_AVAILABILITY_TIME.isNull.or(TB_GROUP.START_AVAILABILITY_TIME.le(time))))
		val endsAfter = TB_GROUP.END_AVAILABILITY_DATE.isNull
			.or(TB_GROUP.END_AVAILABILITY_DATE.gt(date))
			.or(TB_GROUP.END_AVAILABILITY_DATE.eq(date).and(TB_GROUP.END_AVAILABILITY_TIME.isNull.or(TB_GROUP.END_AVAILABILITY_TIME.ge(time))))
		return startsBefore.and(endsAfter)
	}

	private val membersCount: Field<Long> = field(name("members", "members_count"), Long::class.java)
	private val insideMembersCountRaw: Field<Long> = field(name("inside_members", "inside_members_count"), Long::class.java)
	private val fullMembersCount: Field<Long> = field(name("full_members_count"), Long::class.java)
	private val fullInsideMembersCount: Field<Long> = field(name("full_inside_members_count"), Long::class.java)
	private val fullOutsideMembersCount: Field<Long> = field(name("full_outside_members_count"), Long::class.java)

	// Builds the whole WITH participants_groups, inside_members, members SELECT ... in one chain — the
	// dsl.with(...) call must be the very start of the fluent chain, so this can't be split the way the
	// simpler resources' baseSelect() was.
	private fun baseSelect(
		projectId: UUID,
		project: TbProject,
		creator: TbUser,
		editor: TbUser,
		vararg extraFields: SelectField<*>,
	): SelectOnConditionStep<Record> {
		val participantsGroups = participantsGroupsCte(projectId)
		val insideMembers = insideMembersCte(participantsGroups)
		val members = membersCte()
		return dsl.with(participantsGroups, insideMembers, members)
			.select(
				listOf(
					TB_GROUP.asterisk(),
					membersCount.`as`("full_members_count"),
					DSL.coalesce(insideMembersCountRaw, DSL.inline(0L)).`as`("full_inside_members_count"),
					membersCount.minus(DSL.coalesce(insideMembersCountRaw, DSL.inline(0L))).`as`("full_outside_members_count"),
					project.NAME, project.BEGIN_DATE, project.BEGIN_TIME, project.END_DATE, project.END_TIME, project.OPTIONS,
					creator.FIRST_NAME, creator.LAST_NAME, creator.EMAIL, editor.FIRST_NAME, editor.LAST_NAME, editor.EMAIL,
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
				.where(searchConditions(projectId, textSearched, visibilitySearched, presenceSearched, dateTimeSearched))
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
				.where(TB_GROUP.PROJECT_ID.eq(projectId).and(TB_GROUP.ID.`in`(ids)).and(visibleCondition(TB_GROUP.VISIBLE, visibilitySearched)))
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
				.where(searchConditions(projectId, textSearched, visibilitySearched, presenceSearched, dateTimeSearched))
				.orderBy(TB_GROUP.NAME)
				.limit(limit)
		).map { it.toEntity(project, creator, editor, includeCounts = true) }
	}

	fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<GroupEntity> {
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		return Mono.from(
			baseSelect(projectId, project, creator, editor)
				.where(TB_GROUP.PROJECT_ID.eq(projectId).and(TB_GROUP.ID.eq(id)).and(visibleCondition(TB_GROUP.VISIBLE, visibilitySearched)))
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

	fun save(entity: GroupEntity, using: DSLContext): Mono<GroupEntity> = if (entity.id == null) insert(using, entity) else update(using, entity)

	private fun insert(using: DSLContext, entity: GroupEntity): Mono<GroupEntity> {
		val record = using.newRecord(TB_GROUP)
		entity.visible?.let { record.set(TB_GROUP.VISIBLE, it) }
		entity.createdAt?.let { record.set(TB_GROUP.CREATED_DATE, it) }
		entity.creatorId?.let { record.set(TB_GROUP.CREATED_BY, it) }
		entity.lastUpdateAt?.let { record.set(TB_GROUP.LAST_MODIFIED_DATE, it) }
		entity.lastEditorId?.let { record.set(TB_GROUP.LAST_MODIFIED_BY, it) }
		entity.projectId?.let { record.set(TB_GROUP.PROJECT_ID, it) }
		entity.name?.let { record.set(TB_GROUP.NAME, it) }
		entity.startAvailabilityDate?.let { record.set(TB_GROUP.START_AVAILABILITY_DATE, it) }
		entity.startAvailabilityTime?.let { record.set(TB_GROUP.START_AVAILABILITY_TIME, it) }
		entity.endAvailabilityDate?.let { record.set(TB_GROUP.END_AVAILABILITY_DATE, it) }
		entity.endAvailabilityTime?.let { record.set(TB_GROUP.END_AVAILABILITY_TIME, it) }
		return Mono.from(using.insertInto(TB_GROUP).set(record).returning()).map { it.toEntity() }
	}

	private fun update(using: DSLContext, entity: GroupEntity): Mono<GroupEntity> = Mono.from(
		using.update(TB_GROUP)
			.set(TB_GROUP.VISIBLE, entity.visible)
			.set(TB_GROUP.CREATED_DATE, entity.createdAt)
			.set(TB_GROUP.CREATED_BY, entity.creatorId)
			.set(TB_GROUP.LAST_MODIFIED_DATE, entity.lastUpdateAt)
			.set(TB_GROUP.LAST_MODIFIED_BY, entity.lastEditorId)
			.set(TB_GROUP.PROJECT_ID, entity.projectId)
			.set(TB_GROUP.NAME, entity.name)
			.set(TB_GROUP.START_AVAILABILITY_DATE, entity.startAvailabilityDate)
			.set(TB_GROUP.START_AVAILABILITY_TIME, entity.startAvailabilityTime)
			.set(TB_GROUP.END_AVAILABILITY_DATE, entity.endAvailabilityDate)
			.set(TB_GROUP.END_AVAILABILITY_TIME, entity.endAvailabilityTime)
			.where(TB_GROUP.ID.eq(entity.id))
			.returning()
	).map { it.toEntity() }

	fun deleteById(id: UUID): Mono<Unit> = Mono.from(dsl.deleteFrom(TB_GROUP).where(TB_GROUP.ID.eq(id))).map { }

	// includeCounts is false for insert/update's plain INSERT/UPDATE ... RETURNING results, which never
	// carry the members/inside_members/outside_members CTE columns (only real find* queries join those).
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
		id = get(TB_GROUP.ID)
		visible = get(TB_GROUP.VISIBLE)
		createdAt = get(TB_GROUP.CREATED_DATE)
		creatorId = get(TB_GROUP.CREATED_BY)
		creatorFirstName = creator?.let { get(it.FIRST_NAME) }
		creatorLastName = creator?.let { get(it.LAST_NAME) }
		creatorEmail = creator?.let { get(it.EMAIL) }
		lastUpdateAt = get(TB_GROUP.LAST_MODIFIED_DATE)
		lastEditorId = get(TB_GROUP.LAST_MODIFIED_BY)
		lastEditorFirstName = editor?.let { get(it.FIRST_NAME) }
		lastEditorLastName = editor?.let { get(it.LAST_NAME) }
		lastEditorEmail = editor?.let { get(it.EMAIL) }
		this.fullCount = fullCount?.let { get(it)?.toLong() }
		projectId = get(TB_GROUP.PROJECT_ID)
		projectName = project?.let { get(it.NAME) }
		projectStartDate = project?.let { get(it.BEGIN_DATE) }
		projectStartTime = project?.let { get(it.BEGIN_TIME) }
		projectEndDate = project?.let { get(it.END_DATE) }
		projectEndTime = project?.let { get(it.END_TIME) }
		projectOptions = project?.let { get(it.OPTIONS) }
	}
}
