package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbProject
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbUser
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_GROUP
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_GROUP_CONTENT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_MOVEMENT_CONTENT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_PARTICIPANT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_USER
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.routines.references.similarity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.creatorTable
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.editorTable
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.projectTable
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.visibleCondition
import java.time.LocalDate
import java.time.OffsetDateTime
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
import org.jooq.impl.DSL
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.jsonArrayAgg
import org.jooq.impl.DSL.jsonObject
import org.jooq.impl.DSL.key
import org.jooq.impl.DSL.max
import org.jooq.impl.DSL.name
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
class ParticipantJooqRepository(private val dsl: DSLContext) {
	private companion object {
		const val MAX_UNPAGINATED_RESULT = 1000
	}

	private val fgParticipantId: Field<UUID> = field(name("filtered_groups", "participant_id"), UUID::class.java)
	private val fgGroups: Field<JSON> = field(name("filtered_groups", "groups"), JSON::class.java)
	private val fgAvailableGroups: Field<JSON> = field(name("filtered_groups", "available_groups"), JSON::class.java)
	private val fgAvailableGroupsOnDate: Field<JSON> = field(name("filtered_groups", "available_groups_on_date"), JSON::class.java)
	private val fgMinStartAvailability: Field<OffsetDateTime> = field(name("filtered_groups", "min_start_availability"), OffsetDateTime::class.java)
	private val fgMaxEndAvailability: Field<OffsetDateTime> = field(name("filtered_groups", "max_end_availability"), OffsetDateTime::class.java)
	private val lmParticipantId: Field<UUID> = field(name("last_movement", "participant_id"), UUID::class.java)
	private val lmType: Field<String> = field(name("last_movement", "type"), String::class.java)
	// jOOQ's generic field(Name, Class) type-resolution doesn't recognize ZonedDateTime until at least one
	// generated table class (whose codegen forcedTypes registered the ZonedDateTimeConverter) has been
	// loaded — deferred with `by lazy` so it resolves after TB_PARTICIPANT etc. are touched, not at
	// construction time.
	private val lmDateTime: Field<ZonedDateTime> by lazy { field(name("last_movement", "participant_last_movement_date_time"), ZonedDateTime::class.java) }
	private val eighteenYearsAgo: Field<LocalDate?> = DSL.field("CURRENT_DATE - INTERVAL '18 years'", LocalDate::class.java)

	private fun userTable(): TbUser = TB_USER.`as`("user_tb")

	private fun currentlyUsableCondition(startDate: Field<LocalDate?>, startTime: Field<OffsetTime?>, endDate: Field<LocalDate?>, endTime: Field<OffsetTime?>): Condition {
		val started = startDate.isNull.or(startDate.lt(DSL.currentLocalDate())).or(startDate.eq(DSL.currentLocalDate()).and(startTime.isNull.or(startTime.le(DSL.currentOffsetTime()))))
		val notEnded = endDate.isNull.or(endDate.gt(DSL.currentLocalDate())).or(endDate.eq(DSL.currentLocalDate()).and(endTime.isNull.or(endTime.ge(DSL.currentOffsetTime()))))
		return started.and(notEnded)
	}

	// Mirrors ParticipantQueries.DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE / the group_presence_on_date join condition:
	// "COALESCE(date, '-infinity'/'+infinity') cmp X" is logically equivalent to "date IS NULL OR date cmp X".
	private fun dateRangeCondition(startDate: Field<LocalDate?>, startTime: Field<OffsetTime?>, endDate: Field<LocalDate?>, endTime: Field<OffsetTime?>, dateTimeSearched: ZonedDateTime): Condition {
		val date = dateTimeSearched.toLocalDate()
		val time = dateTimeSearched.toOffsetDateTime().toOffsetTime()
		val startsBefore = startDate.isNull.or(startDate.lt(date)).or(startDate.eq(date).and(startTime.isNull.or(startTime.le(time))))
		val endsAfter = endDate.isNull.or(endDate.gt(date)).or(endDate.eq(date).and(endTime.isNull.or(endTime.ge(time))))
		return startsBefore.and(endsAfter)
	}

	// Mirrors ParticipantQueries.WITH_PARTICIPANT_LAST_MOVEMENT. The inner join only matches on date_time
	// equality (not participant_id), exactly as the original raw SQL — replicated as-is.
	private fun lastMovementCte(): CommonTableExpression<*> {
		val plm = dsl.select(max(TB_MOVEMENT.DATE_TIME).`as`("participant_last_movement_date_time"), TB_MOVEMENT_CONTENT.PARTICIPANT_ID)
			.from(TB_MOVEMENT)
			.join(TB_MOVEMENT_CONTENT).on(TB_MOVEMENT_CONTENT.MOVEMENT_ID.eq(TB_MOVEMENT.ID))
			.where(TB_MOVEMENT.VISIBLE.isTrue)
			.groupBy(TB_MOVEMENT_CONTENT.PARTICIPANT_ID)
			.asTable("plm")
		val plmDateTime = field(name("plm", "participant_last_movement_date_time"), ZonedDateTime::class.java)
		val plmParticipantId = field(name("plm", "participant_id"), UUID::class.java)
		return name("last_movement").`as`(
			dsl.select(plmDateTime.`as`("participant_last_movement_date_time"), plmParticipantId.`as`("participant_id"), TB_MOVEMENT.TYPE.`as`("type"))
				.from(TB_MOVEMENT)
				.join(plm).on(plmDateTime.eq(TB_MOVEMENT.DATE_TIME))
		)
	}

	// Mirrors ParticipantQueries.WITH_PARTICIPANT_GROUPS: for each participant in the project, the visible
	// groups they belong to (as JSON {id,name} objects), which ones are currently available, and (when
	// dateTimeSearched is given) which ones are available at that instant.
	private fun filteredGroupsCte(projectId: UUID, dateTimeSearched: ZonedDateTime?): CommonTableExpression<*> {
		val groupPresence = TB_GROUP.`as`("group_presence")
		val groupPresenceOnDate = TB_GROUP.`as`("group_presence_on_date")

		// Mirrors the original SQL's pre-existing operator-precedence quirk: "... AND group_presence_on_date.visible
		// IS TRUE AND :dateTimeSearched IS NULL OR (...)" parses (AND binds tighter than OR) as
		// "(id = ... AND visible AND dateTimeSearched IS NULL) OR (...)". When dateTimeSearched is non-null the
		// first conjunct is always false, collapsing the join condition to just the date-range predicate below,
		// with no id/visible constraint. Replicated as-is (not a fix) for behavioral parity.
		val onDateJoinCondition = if (dateTimeSearched == null) {
			groupPresenceOnDate.ID.eq(TB_GROUP_CONTENT.GROUP_ID).and(groupPresenceOnDate.VISIBLE.isTrue)
		} else {
			dateRangeCondition(groupPresenceOnDate.START_AVAILABILITY_DATE, groupPresenceOnDate.START_AVAILABILITY_TIME, groupPresenceOnDate.END_AVAILABILITY_DATE, groupPresenceOnDate.END_AVAILABILITY_TIME, dateTimeSearched)
		}

		return name("filtered_groups").`as`(
			dsl.select(
				TB_PARTICIPANT.ID.`as`("participant_id"),
				jsonArrayAgg(jsonObject(key("id").value(TB_GROUP.ID), key("name").value(TB_GROUP.NAME))).filterWhere(TB_GROUP.ID.isNotNull).`as`("groups"),
				DSL.field(
					"MIN(CASE WHEN {0} IS NULL THEN '-infinity'::timestamptz ELSE ({0} + COALESCE({1}, '00:00:00.000000'::time))::timestamptz END)",
					OffsetDateTime::class.java,
					groupPresence.START_AVAILABILITY_DATE, groupPresence.START_AVAILABILITY_TIME,
				).`as`("min_start_availability"),
				DSL.field(
					"MAX(CASE WHEN {0} IS NULL THEN '+infinity'::timestamptz ELSE ({0} + COALESCE({1}, '23:59:59.999999'::time))::timestamptz END)",
					OffsetDateTime::class.java,
					groupPresence.END_AVAILABILITY_DATE, groupPresence.END_AVAILABILITY_TIME,
				).`as`("max_end_availability"),
				jsonArrayAgg(groupPresence.ID).filterWhere(groupPresence.ID.isNotNull).`as`("available_groups"),
				jsonArrayAgg(groupPresenceOnDate.ID).filterWhere(groupPresenceOnDate.ID.isNotNull).`as`("available_groups_on_date"),
			)
				.from(TB_PARTICIPANT)
				.leftJoin(TB_GROUP_CONTENT).on(TB_GROUP_CONTENT.PARTICIPANT_ID.eq(TB_PARTICIPANT.ID))
				.leftJoin(TB_GROUP).on(TB_GROUP.ID.eq(TB_GROUP_CONTENT.GROUP_ID).and(TB_GROUP.VISIBLE.isTrue))
				.leftJoin(groupPresence).on(
					groupPresence.ID.eq(TB_GROUP_CONTENT.GROUP_ID).and(groupPresence.VISIBLE.isTrue)
						.and(currentlyUsableCondition(groupPresence.START_AVAILABILITY_DATE, groupPresence.START_AVAILABILITY_TIME, groupPresence.END_AVAILABILITY_DATE, groupPresence.END_AVAILABILITY_TIME))
				)
				.leftJoin(groupPresenceOnDate).on(onDateJoinCondition)
				.where(TB_PARTICIPANT.PROJECT_ID.eq(projectId))
				.groupBy(TB_PARTICIPANT.ID)
		)
	}

	private fun availabilityCondition(): Condition = DSL.condition(
		"""
		(
			COALESCE({0}, CAST({4} AS DATE), '+infinity'::DATE) < CURRENT_DATE
			OR (COALESCE({0}, CAST({4} AS DATE), '+infinity'::DATE) = CURRENT_DATE AND COALESCE({1}, CAST({4} AS TIME), '00:00:00.000000'::TIME) <= CURRENT_TIME)
		) AND (
			COALESCE({2}, CAST({5} AS DATE), '-infinity'::DATE) > CURRENT_DATE
			OR (COALESCE({2}, CAST({5} AS DATE), '-infinity'::DATE) = CURRENT_DATE AND COALESCE({3}, CAST({5} AS TIME), '23:59:59.999999'::TIME) >= CURRENT_TIME)
		)
		""",
		TB_PARTICIPANT.START_AVAILABILITY_DATE, TB_PARTICIPANT.START_AVAILABILITY_TIME,
		TB_PARTICIPANT.END_AVAILABILITY_DATE, TB_PARTICIPANT.END_AVAILABILITY_TIME,
		fgMinStartAvailability, fgMaxEndAvailability,
	)

	private fun dateInParticipantRangeCondition(dateTimeSearched: ZonedDateTime): Condition {
		val hasNoGroupsOrAvailableOnDate = DSL.condition(
			"({0} IS NULL OR json_array_length({0}) = 0 OR ({1} IS NOT NULL AND json_array_length({1}) > 0))",
			fgGroups, fgAvailableGroupsOnDate,
		)
		val dateRange = dateRangeCondition(TB_PARTICIPANT.START_AVAILABILITY_DATE, TB_PARTICIPANT.START_AVAILABILITY_TIME, TB_PARTICIPANT.END_AVAILABILITY_DATE, TB_PARTICIPANT.END_AVAILABILITY_TIME, dateTimeSearched)
		return hasNoGroupsOrAvailableOnDate.and(dateRange)
	}

	private fun similarityScoreField(textSearched: String?): Field<Float> =
		(if (textSearched == null) DSL.inline(1f) else similarity(TB_PARTICIPANT.SEARCH_TEXT, DSL.`val`(textSearched))).`as`("similarity_score")

	private fun searchConditions(
		projectId: UUID,
		textSearched: String?,
		isMajor: Boolean?,
		typeSearched: ParticipantTypeEnum?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		presenceSearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
	): Condition {
		val conditions = mutableListOf(
			TB_PARTICIPANT.PURGED.isFalse,
			TB_PARTICIPANT.PROJECT_ID.eq(projectId),
		)
		textSearched?.let { conditions += similarity(TB_PARTICIPANT.SEARCH_TEXT, DSL.`val`(it)).gt(0f) }
		isMajor?.let {
			val isMajorCondition = TB_PARTICIPANT.BIRTHDAY.le(eighteenYearsAgo)
			conditions += if (it) isMajorCondition else isMajorCondition.not()
		}
		typeSearched?.let { conditions += TB_PARTICIPANT.TYPE.eq(it) }
		conditions += visibleCondition(TB_PARTICIPANT.VISIBLE, visibilitySearched)
		availabilitySearched?.let {
			val cond = availabilityCondition()
			conditions += if (it) cond else cond.not()
		}
		presenceSearched?.let {
			val isOutOrNull = lmType.isNull.or(lmType.eq("OUT"))
			conditions += if (it) isOutOrNull.not() else isOutOrNull
		}
		dateTimeSearched?.let { conditions += dateInParticipantRangeCondition(it) }
		return DSL.and(conditions)
	}

	// Builds the WITH last_movement, filtered_groups SELECT ... FROM tb_participant + all the joins shared by
	// every read query except findAllWithBirthday (which needs neither CTE) and countAll (which needs no
	// user/project/creator/editor join at all).
	private fun fullSelect(
		projectId: UUID,
		dateTimeSearched: ZonedDateTime?,
		user: TbUser,
		project: TbProject,
		creator: TbUser,
		editor: TbUser,
		vararg extraFields: SelectField<*>,
	): SelectOnConditionStep<Record> {
		val lastMovement = lastMovementCte()
		val filteredGroups = filteredGroupsCte(projectId, dateTimeSearched)
		return dsl.with(lastMovement, filteredGroups)
			.select(
				listOf(
					TB_PARTICIPANT.asterisk(),
					user.FIRST_NAME, user.LAST_NAME, user.EMAIL,
					lmType, lmDateTime,
					fgGroups, fgAvailableGroups,
					project.NAME, project.BEGIN_DATE, project.BEGIN_TIME, project.END_DATE, project.END_TIME, project.OPTIONS,
					creator.FIRST_NAME, creator.LAST_NAME, creator.EMAIL, editor.FIRST_NAME, editor.LAST_NAME, editor.EMAIL,
				) + extraFields.toList()
			)
			.from(TB_PARTICIPANT)
			.leftJoin(user).on(TB_PARTICIPANT.USER_ID.eq(user.ID).and(user.PURGED.isFalse).and(user.VISIBLE.isTrue))
			.leftJoin(lastMovement).on(lmParticipantId.eq(TB_PARTICIPANT.ID))
			.leftJoin(filteredGroups).on(fgParticipantId.eq(TB_PARTICIPANT.ID))
			.join(project).on(TB_PARTICIPANT.PROJECT_ID.eq(project.ID).and(project.VISIBLE.isTrue))
			.leftJoin(creator).on(TB_PARTICIPANT.CREATED_BY.eq(creator.ID))
			.leftJoin(editor).on(TB_PARTICIPANT.LAST_MODIFIED_BY.eq(editor.ID))
	}

	fun findAll(
		projectId: UUID,
		textSearched: String?,
		isMajor: Boolean?,
		typeSearched: ParticipantTypeEnum?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		presenceSearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
		limit: Int,
		offset: Int,
	): Flux<ParticipantEntity> {
		val user = userTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		val similarityScore = similarityScoreField(textSearched)
		val fullCount = count().over().`as`("full_count")
		return Flux.from(
			fullSelect(projectId, dateTimeSearched, user, project, creator, editor, similarityScore, fullCount)
				.where(searchConditions(projectId, textSearched, isMajor, typeSearched, visibilitySearched, availabilitySearched, presenceSearched, dateTimeSearched))
				.orderBy(similarityScore.desc(), TB_PARTICIPANT.LAST_NAME)
				.limit(limit).offset(offset)
		).map { it.toEntity(user, project, creator, editor, includeGroups = true, includeLastMovement = true, fullCount = fullCount) }
	}

	fun countAll(
		projectId: UUID,
		textSearched: String?,
		isMajor: Boolean?,
		typeSearched: ParticipantTypeEnum?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		presenceSearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
	): Mono<Long> {
		val lastMovement = lastMovementCte()
		val filteredGroups = filteredGroupsCte(projectId, dateTimeSearched)
		return Mono.from(
			dsl.with(lastMovement, filteredGroups)
				.select(count(TB_PARTICIPANT.ID))
				.from(TB_PARTICIPANT)
				.leftJoin(lastMovement).on(lmParticipantId.eq(TB_PARTICIPANT.ID))
				.leftJoin(filteredGroups).on(fgParticipantId.eq(TB_PARTICIPANT.ID))
				.where(searchConditions(projectId, textSearched, isMajor, typeSearched, visibilitySearched, availabilitySearched, presenceSearched, dateTimeSearched))
		).map { it.value1()!!.toLong() }
	}

	fun findAllByGroupId(
		projectId: UUID,
		groupId: UUID,
		textSearched: String?,
		isMajor: Boolean?,
		typeSearched: ParticipantTypeEnum?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		presenceSearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
		limit: Int,
		offset: Int,
	): Flux<ParticipantEntity> {
		val user = userTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		val similarityScore = similarityScoreField(textSearched)
		val fullCount = count().over().`as`("full_count")
		return Flux.from(
			fullSelect(projectId, dateTimeSearched, user, project, creator, editor, similarityScore, fullCount)
				.join(TB_GROUP_CONTENT).on(TB_GROUP_CONTENT.PARTICIPANT_ID.eq(TB_PARTICIPANT.ID).and(TB_GROUP_CONTENT.GROUP_ID.eq(groupId)))
				.where(searchConditions(projectId, textSearched, isMajor, typeSearched, visibilitySearched, availabilitySearched, presenceSearched, dateTimeSearched))
				.orderBy(similarityScore.desc(), TB_PARTICIPANT.LAST_NAME)
				.limit(limit).offset(offset)
		).map { it.toEntity(user, project, creator, editor, includeGroups = true, includeLastMovement = true, fullCount = fullCount) }
	}

	fun findAllWithBirthday(projectId: UUID, visibilitySearched: Boolean?): Flux<ParticipantEntity> {
		val user = userTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		return Flux.from(
			dsl.select(
				listOf(
					TB_PARTICIPANT.asterisk(),
					user.FIRST_NAME, user.LAST_NAME, user.EMAIL,
					project.NAME, project.BEGIN_DATE, project.BEGIN_TIME, project.END_DATE, project.END_TIME, project.OPTIONS,
					creator.FIRST_NAME, creator.LAST_NAME, creator.EMAIL, editor.FIRST_NAME, editor.LAST_NAME, editor.EMAIL,
				)
			)
				.from(TB_PARTICIPANT)
				.leftJoin(user).on(TB_PARTICIPANT.USER_ID.eq(user.ID).and(user.PURGED.isFalse).and(user.VISIBLE.isTrue))
				.join(project).on(TB_PARTICIPANT.PROJECT_ID.eq(project.ID).and(project.VISIBLE.isTrue))
				.leftJoin(creator).on(TB_PARTICIPANT.CREATED_BY.eq(creator.ID))
				.leftJoin(editor).on(TB_PARTICIPANT.LAST_MODIFIED_BY.eq(editor.ID))
				.where(
					TB_PARTICIPANT.PURGED.isFalse
						.and(TB_PARTICIPANT.PROJECT_ID.eq(projectId))
						.and(DSL.condition("EXTRACT(MONTH FROM {0}) = EXTRACT(MONTH FROM CURRENT_DATE)", TB_PARTICIPANT.BIRTHDAY))
						.and(DSL.condition("EXTRACT(DAY FROM {0}) = EXTRACT(DAY FROM CURRENT_DATE)", TB_PARTICIPANT.BIRTHDAY))
						.and(visibleCondition(TB_PARTICIPANT.VISIBLE, visibilitySearched))
				)
				.limit(MAX_UNPAGINATED_RESULT)
		).map { it.toEntity(user, project, creator, editor) }
	}

	fun findAllByIds(projectId: UUID, ids: List<UUID>, visibilitySearched: Boolean?, dateTimeSearched: ZonedDateTime?): Flux<ParticipantEntity> {
		if (ids.isEmpty()) return Flux.empty()
		val user = userTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		return Flux.from(
			fullSelect(projectId, dateTimeSearched, user, project, creator, editor)
				.where(
					TB_PARTICIPANT.PURGED.isFalse
						.and(TB_PARTICIPANT.PROJECT_ID.eq(projectId))
						.and(TB_PARTICIPANT.ID.`in`(ids))
						.and(visibleCondition(TB_PARTICIPANT.VISIBLE, visibilitySearched))
				)
		).map { it.toEntity(user, project, creator, editor, includeGroups = true, includeLastMovement = true) }
	}

	fun findByUserId(projectId: UUID, userId: UUID, dateTimeSearched: ZonedDateTime?): Flux<ParticipantEntity> {
		val user = userTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		return Flux.from(
			fullSelect(projectId, dateTimeSearched, user, project, creator, editor)
				.where(TB_PARTICIPANT.PURGED.isFalse.and(TB_PARTICIPANT.PROJECT_ID.eq(projectId)).and(TB_PARTICIPANT.USER_ID.eq(userId)))
				.limit(MAX_UNPAGINATED_RESULT)
		).map { it.toEntity(user, project, creator, editor, includeGroups = true, includeLastMovement = true) }
	}

	fun findWithLimit(
		projectId: UUID,
		textSearched: String?,
		isMajor: Boolean?,
		typeSearched: ParticipantTypeEnum?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		presenceSearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
		limit: Int,
	): Flux<ParticipantEntity> {
		val user = userTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		val similarityScore = similarityScoreField(textSearched)
		return Flux.from(
			fullSelect(projectId, dateTimeSearched, user, project, creator, editor, similarityScore)
				.where(searchConditions(projectId, textSearched, isMajor, typeSearched, visibilitySearched, availabilitySearched, presenceSearched, dateTimeSearched))
				.orderBy(similarityScore.desc(), TB_PARTICIPANT.LAST_NAME)
				.limit(limit)
		).map { it.toEntity(user, project, creator, editor, includeGroups = true, includeLastMovement = true) }
	}

	fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?, dateTimeSearched: ZonedDateTime?): Mono<ParticipantEntity> {
		val user = userTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		return Mono.from(
			fullSelect(projectId, dateTimeSearched, user, project, creator, editor)
				.where(
					TB_PARTICIPANT.PURGED.isFalse
						.and(TB_PARTICIPANT.PROJECT_ID.eq(projectId))
						.and(TB_PARTICIPANT.ID.eq(id))
						.and(visibleCondition(TB_PARTICIPANT.VISIBLE, visibilitySearched))
				)
		).map { it.toEntity(user, project, creator, editor, includeGroups = true, includeLastMovement = true) }
	}

	fun updateAllEndAvailability(ids: List<UUID>, endAvailabilityTime: OffsetTime?, endAvailabilityDate: LocalDate?): Flux<ParticipantEntity> {
		if (ids.isEmpty()) return Flux.empty()
		return Flux.from(
			dsl.update(TB_PARTICIPANT)
				.set(TB_PARTICIPANT.END_AVAILABILITY_TIME, endAvailabilityTime)
				.set(TB_PARTICIPANT.END_AVAILABILITY_DATE, endAvailabilityDate)
				.where(TB_PARTICIPANT.ID.`in`(ids))
				.returning()
		).map { it.toEntity() }
	}

	fun findUnusedSince(dateThreshold: LocalDate): Flux<UUID> {
		val lastMovement = lastMovementCte()
		return Flux.from(
			dsl.with(lastMovement)
				.select(TB_PARTICIPANT.ID)
				.from(TB_PARTICIPANT)
				.leftJoin(lastMovement).on(lmParticipantId.eq(TB_PARTICIPANT.ID))
				.where(
					lmDateTime.isNull.or(DSL.condition("{0} < {1}", lmDateTime, DSL.`val`(dateThreshold)))
						.and(DSL.condition("{0} < {1}", TB_PARTICIPANT.LAST_MODIFIED_DATE, DSL.`val`(dateThreshold)))
				)
		).map { it.value1()!! }
	}

	fun save(entity: ParticipantEntity): Mono<ParticipantEntity> = save(entity, dsl)

	fun save(entity: ParticipantEntity, using: DSLContext): Mono<ParticipantEntity> = if (entity.id == null) insert(using, entity) else update(using, entity)

	fun saveAll(entities: Iterable<ParticipantEntity>): Flux<ParticipantEntity> = Flux.fromIterable(entities).flatMap { save(it) }

	private fun insert(using: DSLContext, entity: ParticipantEntity): Mono<ParticipantEntity> {
		val record = using.newRecord(TB_PARTICIPANT)
		entity.visible?.let { record.set(TB_PARTICIPANT.VISIBLE, it) }
		entity.createdAt?.let { record.set(TB_PARTICIPANT.CREATED_DATE, it) }
		entity.creatorId?.let { record.set(TB_PARTICIPANT.CREATED_BY, it) }
		entity.lastUpdateAt?.let { record.set(TB_PARTICIPANT.LAST_MODIFIED_DATE, it) }
		entity.lastEditorId?.let { record.set(TB_PARTICIPANT.LAST_MODIFIED_BY, it) }
		entity.projectId?.let { record.set(TB_PARTICIPANT.PROJECT_ID, it) }
		entity.firstName?.let { record.set(TB_PARTICIPANT.FIRST_NAME, it) }
		entity.lastName?.let { record.set(TB_PARTICIPANT.LAST_NAME, it) }
		entity.birthday?.let { record.set(TB_PARTICIPANT.BIRTHDAY, it) }
		entity.type?.let { record.set(TB_PARTICIPANT.TYPE, it) }
		entity.startAvailabilityDate?.let { record.set(TB_PARTICIPANT.START_AVAILABILITY_DATE, it) }
		entity.startAvailabilityTime?.let { record.set(TB_PARTICIPANT.START_AVAILABILITY_TIME, it) }
		entity.endAvailabilityDate?.let { record.set(TB_PARTICIPANT.END_AVAILABILITY_DATE, it) }
		entity.endAvailabilityTime?.let { record.set(TB_PARTICIPANT.END_AVAILABILITY_TIME, it) }
		entity.userId?.let { record.set(TB_PARTICIPANT.USER_ID, it) }
		entity.purged?.let { record.set(TB_PARTICIPANT.PURGED, it) }
		return Mono.from(using.insertInto(TB_PARTICIPANT).set(record).returning()).map { it.toEntity() }
	}

	private fun update(using: DSLContext, entity: ParticipantEntity): Mono<ParticipantEntity> = Mono.from(
		using.update(TB_PARTICIPANT)
			.set(TB_PARTICIPANT.VISIBLE, entity.visible)
			.set(TB_PARTICIPANT.CREATED_DATE, entity.createdAt)
			.set(TB_PARTICIPANT.CREATED_BY, entity.creatorId)
			.set(TB_PARTICIPANT.LAST_MODIFIED_DATE, entity.lastUpdateAt)
			.set(TB_PARTICIPANT.LAST_MODIFIED_BY, entity.lastEditorId)
			.set(TB_PARTICIPANT.PROJECT_ID, entity.projectId)
			.set(TB_PARTICIPANT.FIRST_NAME, entity.firstName)
			.set(TB_PARTICIPANT.LAST_NAME, entity.lastName)
			.set(TB_PARTICIPANT.BIRTHDAY, entity.birthday)
			.set(TB_PARTICIPANT.TYPE, entity.type)
			.set(TB_PARTICIPANT.START_AVAILABILITY_DATE, entity.startAvailabilityDate)
			.set(TB_PARTICIPANT.START_AVAILABILITY_TIME, entity.startAvailabilityTime)
			.set(TB_PARTICIPANT.END_AVAILABILITY_DATE, entity.endAvailabilityDate)
			.set(TB_PARTICIPANT.END_AVAILABILITY_TIME, entity.endAvailabilityTime)
			.set(TB_PARTICIPANT.USER_ID, entity.userId)
			.set(TB_PARTICIPANT.PURGED, entity.purged)
			.where(TB_PARTICIPANT.ID.eq(entity.id))
			.returning()
	).map { it.toEntity() }

	fun deleteById(id: UUID): Mono<Unit> = Mono.from(dsl.deleteFrom(TB_PARTICIPANT).where(TB_PARTICIPANT.ID.eq(id))).map { }

	fun deleteAllById(ids: List<UUID>): Mono<Unit> = Mono.from(dsl.deleteFrom(TB_PARTICIPANT).where(TB_PARTICIPANT.ID.`in`(ids))).map { }

	// includeGroups/includeLastMovement are false for insert/update/updateAllEndAvailability's plain
	// INSERT/UPDATE ... RETURNING results, which never carry the CTE-joined columns (only real find* queries
	// join those) — matches ParticipantEntity's own "[]" constructor defaults for groups/availableGroups.
	private fun Record.toEntity(
		user: TbUser? = null,
		project: TbProject? = null,
		creator: TbUser? = null,
		editor: TbUser? = null,
		includeGroups: Boolean = false,
		includeLastMovement: Boolean = false,
		fullCount: Field<Int>? = null,
	): ParticipantEntity = ParticipantEntity(
		firstName = get(TB_PARTICIPANT.FIRST_NAME),
		lastName = get(TB_PARTICIPANT.LAST_NAME),
		birthday = get(TB_PARTICIPANT.BIRTHDAY),
		type = get(TB_PARTICIPANT.TYPE),
		groups = if (includeGroups) get(fgGroups)?.data() ?: "[]" else "[]",
		availableGroups = if (includeGroups) get(fgAvailableGroups)?.data() ?: "[]" else "[]",
		lastMovementType = if (includeLastMovement) get(lmType)?.let(MovementTypeEnum::valueOf) else null,
		lastMovementDateTime = if (includeLastMovement) get(lmDateTime) else null,
		startAvailabilityDate = get(TB_PARTICIPANT.START_AVAILABILITY_DATE),
		startAvailabilityTime = get(TB_PARTICIPANT.START_AVAILABILITY_TIME),
		endAvailabilityDate = get(TB_PARTICIPANT.END_AVAILABILITY_DATE),
		endAvailabilityTime = get(TB_PARTICIPANT.END_AVAILABILITY_TIME),
		userId = get(TB_PARTICIPANT.USER_ID),
		userFirstName = user?.let { get(it.FIRST_NAME) },
		userLastName = user?.let { get(it.LAST_NAME) },
		userEmail = user?.let { get(it.EMAIL) },
		purged = get(TB_PARTICIPANT.PURGED),
	).apply {
		id = get(TB_PARTICIPANT.ID)
		visible = get(TB_PARTICIPANT.VISIBLE)
		createdAt = get(TB_PARTICIPANT.CREATED_DATE)
		creatorId = get(TB_PARTICIPANT.CREATED_BY)
		creatorFirstName = creator?.let { get(it.FIRST_NAME) }
		creatorLastName = creator?.let { get(it.LAST_NAME) }
		creatorEmail = creator?.let { get(it.EMAIL) }
		lastUpdateAt = get(TB_PARTICIPANT.LAST_MODIFIED_DATE)
		lastEditorId = get(TB_PARTICIPANT.LAST_MODIFIED_BY)
		lastEditorFirstName = editor?.let { get(it.FIRST_NAME) }
		lastEditorLastName = editor?.let { get(it.LAST_NAME) }
		lastEditorEmail = editor?.let { get(it.EMAIL) }
		this.fullCount = fullCount?.let { get(it)?.toLong() }
		projectId = get(TB_PARTICIPANT.PROJECT_ID)
		projectName = project?.let { get(it.NAME) }
		projectStartDate = project?.let { get(it.BEGIN_DATE) }
		projectStartTime = project?.let { get(it.BEGIN_TIME) }
		projectEndDate = project?.let { get(it.END_DATE) }
		projectEndTime = project?.let { get(it.END_TIME) }
		projectOptions = project?.let { get(it.OPTIONS) }
	}
}
