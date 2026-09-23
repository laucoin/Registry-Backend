package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantSortFieldEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantSortFieldEnum.BIRTHDAY
import fr.laucoin.registry.backend.domain.enumeration.ParticipantSortFieldEnum.CREATED_DATE
import fr.laucoin.registry.backend.domain.enumeration.ParticipantSortFieldEnum.FIRST_NAME
import fr.laucoin.registry.backend.domain.enumeration.ParticipantSortFieldEnum.LAST_MODIFIED_DATE
import fr.laucoin.registry.backend.domain.enumeration.ParticipantSortFieldEnum.LAST_NAME
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.SortDirectionEnum.DESC
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.routines.references.similarity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbProject
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbUser
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_GROUP
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_GROUP_CONTENT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_MOVEMENT_CONTENT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_PARTICIPANT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_USER
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
import org.jooq.OrderField
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
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.util.UUID

@Repository
class ParticipantJooqRepository(private val dsl: DSLContext) {
	private companion object {
		const val MAX_UNPAGINATED_RESULT = 1000
	}

	private val fgParticipantId: Field<UUID> = field(name("filtered_groups", "participant_id"), UUID::class.java)
	private val fgGroups: Field<JSON> = field(name("filtered_groups", "groups"), JSON::class.java)
	private val fgAvailableGroups: Field<JSON> = field(name("filtered_groups", "available_groups"), JSON::class.java)
	private val fgAvailableGroupsOnDate: Field<JSON> =
		field(name("filtered_groups", "available_groups_on_date"), JSON::class.java)
	private val fgMinStartAvailability: Field<OffsetDateTime> =
		field(name("filtered_groups", "min_start_availability"), OffsetDateTime::class.java)
	private val fgMaxEndAvailability: Field<OffsetDateTime> =
		field(name("filtered_groups", "max_end_availability"), OffsetDateTime::class.java)
	private val lmParticipantId: Field<UUID> = field(name("last_movement", "participant_id"), UUID::class.java)
	private val lmType: Field<String> = field(name("last_movement", "type"), String::class.java)
	private val lmDateTime: Field<ZonedDateTime> by lazy {
		field(
			name(
				"last_movement",
				"participant_last_movement_date_time"
			), ZonedDateTime::class.java
		)
	}
	private val eighteenYearsAgo: Field<LocalDate?> =
		DSL.field("CURRENT_DATE - INTERVAL '18 years'", LocalDate::class.java)
	private val columns = GenericColumns(
		TB_PARTICIPANT.ID,
		TB_PARTICIPANT.VISIBLE,
		TB_PARTICIPANT.CREATED_DATE,
		TB_PARTICIPANT.CREATED_BY,
		TB_PARTICIPANT.LAST_MODIFIED_DATE,
		TB_PARTICIPANT.LAST_MODIFIED_BY,
	)

	private fun userTable(): TbUser = TB_USER.`as`("user_tb")

	private fun lastMovementCte(using: DSLContext = dsl): CommonTableExpression<*> {
		val plm = using.select(
			max(TB_MOVEMENT.DATE_TIME).`as`("participant_last_movement_date_time"),
			TB_MOVEMENT_CONTENT.PARTICIPANT_ID,
		)
			.from(TB_MOVEMENT)
			.join(TB_MOVEMENT_CONTENT).on(TB_MOVEMENT_CONTENT.MOVEMENT_ID.eq(TB_MOVEMENT.ID))
			.where(TB_MOVEMENT.VISIBLE.isTrue)
			.groupBy(TB_MOVEMENT_CONTENT.PARTICIPANT_ID)
			.asTable("plm")
		val plmDateTime = field(name("plm", "participant_last_movement_date_time"), ZonedDateTime::class.java)
		val plmParticipantId = field(name("plm", "participant_id"), UUID::class.java)
		return name("last_movement").`as`(
			using.select(
				plmDateTime.`as`("participant_last_movement_date_time"),
				plmParticipantId.`as`("participant_id"),
				TB_MOVEMENT.TYPE.`as`("type")
			)
				.from(TB_MOVEMENT)
				.join(plm).on(plmDateTime.eq(TB_MOVEMENT.DATE_TIME))
		)
	}

	private fun filteredGroupsCte(
		projectId: UUID,
		dateTimeSearched: ZonedDateTime?,
		using: DSLContext = dsl
	): CommonTableExpression<*> {
		val groupPresence = TB_GROUP.`as`("group_presence")
		val groupPresenceOnDate = TB_GROUP.`as`("group_presence_on_date")

		val onDateJoinCondition = if (dateTimeSearched == null) {
			groupPresenceOnDate.ID.eq(TB_GROUP_CONTENT.GROUP_ID).and(groupPresenceOnDate.VISIBLE.isTrue)
		} else {
			activeAtCondition(
				groupPresenceOnDate.START_AVAILABILITY_DATE,
				groupPresenceOnDate.START_AVAILABILITY_TIME,
				groupPresenceOnDate.END_AVAILABILITY_DATE,
				groupPresenceOnDate.END_AVAILABILITY_TIME,
				dateTimeSearched
			)
		}

		return name("filtered_groups").`as`(
			using.select(
				TB_PARTICIPANT.ID.`as`("participant_id"),
				jsonArrayAgg(jsonObject(key("id").value(TB_GROUP.ID), key("name").value(TB_GROUP.NAME))).filterWhere(
					TB_GROUP.ID.isNotNull
				).`as`("groups"),
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
				jsonArrayAgg(groupPresenceOnDate.ID).filterWhere(groupPresenceOnDate.ID.isNotNull)
					.`as`("available_groups_on_date"),
			)
				.from(TB_PARTICIPANT)
				.leftJoin(TB_GROUP_CONTENT).on(TB_GROUP_CONTENT.PARTICIPANT_ID.eq(TB_PARTICIPANT.ID))
				.leftJoin(TB_GROUP).on(TB_GROUP.ID.eq(TB_GROUP_CONTENT.GROUP_ID).and(TB_GROUP.VISIBLE.isTrue))
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
		val dateRange = activeAtCondition(
			TB_PARTICIPANT.START_AVAILABILITY_DATE,
			TB_PARTICIPANT.START_AVAILABILITY_TIME,
			TB_PARTICIPANT.END_AVAILABILITY_DATE,
			TB_PARTICIPANT.END_AVAILABILITY_TIME,
			dateTimeSearched
		)
		return hasNoGroupsOrAvailableOnDate.and(dateRange)
	}

	private fun similarityScoreField(textSearched: String?): Field<Float> =
		(if (textSearched == null) DSL.inline(1f) else similarity(
			TB_PARTICIPANT.SEARCH_TEXT,
			DSL.`val`(textSearched)
		)).`as`("similarity_score")

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

	private fun fullSelect(
		projectId: UUID,
		dateTimeSearched: ZonedDateTime?,
		user: TbUser,
		project: TbProject,
		creator: TbUser,
		editor: TbUser,
		vararg extraFields: SelectField<*>,
		using: DSLContext = dsl,
	): SelectOnConditionStep<Record> {
		val lastMovement = lastMovementCte(using)
		val filteredGroups = filteredGroupsCte(projectId, dateTimeSearched, using)
		return using.with(lastMovement, filteredGroups)
			.select(
				listOf(
					TB_PARTICIPANT.asterisk(),
					user.FIRST_NAME,
					user.LAST_NAME,
					user.EMAIL,
					lmType,
					lmDateTime,
					fgGroups,
					fgAvailableGroups,
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
			.from(TB_PARTICIPANT)
			.leftJoin(user).on(TB_PARTICIPANT.USER_ID.eq(user.ID).and(user.PURGED.isFalse).and(user.VISIBLE.isTrue))
			.leftJoin(lastMovement).on(lmParticipantId.eq(TB_PARTICIPANT.ID))
			.leftJoin(filteredGroups).on(fgParticipantId.eq(TB_PARTICIPANT.ID))
			.join(project).on(TB_PARTICIPANT.PROJECT_ID.eq(project.ID).and(project.VISIBLE.isTrue))
			.leftJoin(creator).on(TB_PARTICIPANT.CREATED_BY.eq(creator.ID))
			.leftJoin(editor).on(TB_PARTICIPANT.LAST_MODIFIED_BY.eq(editor.ID))
	}

	private fun ParticipantSortFieldEnum.toJooqField(): Field<*> = when (this) {
		FIRST_NAME -> TB_PARTICIPANT.FIRST_NAME
		LAST_NAME -> TB_PARTICIPANT.LAST_NAME
		BIRTHDAY -> TB_PARTICIPANT.BIRTHDAY
		CREATED_DATE -> TB_PARTICIPANT.CREATED_DATE
		LAST_MODIFIED_DATE -> TB_PARTICIPANT.LAST_MODIFIED_DATE
	}

	private fun orderFields(
		similarityScore: Field<Float>,
		sortFields: List<SortModel<ParticipantSortFieldEnum>>,
	): List<OrderField<*>> {
		val fields = mutableListOf<OrderField<*>>(similarityScore.desc())
		sortFields.forEach { fields += if (it.direction == DESC) it.field.toJooqField().desc() else it.field.toJooqField().asc() }
		fields += TB_PARTICIPANT.LAST_NAME
		return fields
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
		sortFields: List<SortModel<ParticipantSortFieldEnum>> = emptyList(),
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
				.orderBy(orderFields(similarityScore, sortFields))
				.limit(limit).offset(offset)
		).map {
			it.toEntity(
				user,
				project,
				creator,
				editor,
				includeGroups = true,
				includeLastMovement = true,
				fullCount = fullCount
			)
		}
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
				.where(
					searchConditions(
						projectId,
						textSearched,
						isMajor,
						typeSearched,
						visibilitySearched,
						availabilitySearched,
						presenceSearched,
						dateTimeSearched
					)
				)
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
				.join(TB_GROUP_CONTENT)
				.on(TB_GROUP_CONTENT.PARTICIPANT_ID.eq(TB_PARTICIPANT.ID).and(TB_GROUP_CONTENT.GROUP_ID.eq(groupId)))
				.where(
					searchConditions(
						projectId,
						textSearched,
						isMajor,
						typeSearched,
						visibilitySearched,
						availabilitySearched,
						presenceSearched,
						dateTimeSearched
					)
				)
				.orderBy(similarityScore.desc(), TB_PARTICIPANT.LAST_NAME)
				.limit(limit).offset(offset)
		).map {
			it.toEntity(
				user,
				project,
				creator,
				editor,
				includeGroups = true,
				includeLastMovement = true,
				fullCount = fullCount
			)
		}
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
					user.FIRST_NAME,
					user.LAST_NAME,
					user.EMAIL,
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
						.and(
							DSL.condition(
								"EXTRACT(MONTH FROM {0}) = EXTRACT(MONTH FROM CURRENT_DATE)",
								TB_PARTICIPANT.BIRTHDAY
							)
						)
						.and(
							DSL.condition(
								"EXTRACT(DAY FROM {0}) = EXTRACT(DAY FROM CURRENT_DATE)",
								TB_PARTICIPANT.BIRTHDAY
							)
						)
						.and(visibleCondition(TB_PARTICIPANT.VISIBLE, visibilitySearched))
				)
				.limit(MAX_UNPAGINATED_RESULT)
		).map { it.toEntity(user, project, creator, editor) }
	}

	fun findAllByIds(
		projectId: UUID,
		ids: List<UUID>,
		visibilitySearched: Boolean?,
		dateTimeSearched: ZonedDateTime?
	): Flux<ParticipantEntity> {
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

	fun findAllByUserId(userId: UUID): Flux<ParticipantEntity> {
		val user = userTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		return Flux.from(
			dsl.select(
				TB_PARTICIPANT.asterisk(),
				user.FIRST_NAME,
				user.LAST_NAME,
				user.EMAIL,
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
			)
				.from(TB_PARTICIPANT)
				.leftJoin(user).on(TB_PARTICIPANT.USER_ID.eq(user.ID))
				.join(project).on(TB_PARTICIPANT.PROJECT_ID.eq(project.ID).and(project.VISIBLE.isTrue))
				.leftJoin(creator).on(TB_PARTICIPANT.CREATED_BY.eq(creator.ID))
				.leftJoin(editor).on(TB_PARTICIPANT.LAST_MODIFIED_BY.eq(editor.ID))
				.where(TB_PARTICIPANT.PURGED.isFalse.and(TB_PARTICIPANT.USER_ID.eq(userId)))
		).map { it.toEntity(user, project, creator, editor) }
	}

	fun findByUserId(projectId: UUID, userId: UUID, dateTimeSearched: ZonedDateTime?): Flux<ParticipantEntity> {
		val user = userTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		return Flux.from(
			fullSelect(projectId, dateTimeSearched, user, project, creator, editor)
				.where(
					TB_PARTICIPANT.PURGED.isFalse.and(TB_PARTICIPANT.PROJECT_ID.eq(projectId))
						.and(TB_PARTICIPANT.USER_ID.eq(userId))
				)
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
				.where(
					searchConditions(
						projectId,
						textSearched,
						isMajor,
						typeSearched,
						visibilitySearched,
						availabilitySearched,
						presenceSearched,
						dateTimeSearched
					)
				)
				.orderBy(similarityScore.desc(), TB_PARTICIPANT.LAST_NAME)
				.limit(limit)
		).map { it.toEntity(user, project, creator, editor, includeGroups = true, includeLastMovement = true) }
	}

	fun findById(
		projectId: UUID,
		id: UUID,
		visibilitySearched: Boolean?,
		dateTimeSearched: ZonedDateTime?
	): Mono<ParticipantEntity> =
		findById(projectId, id, visibilitySearched, dateTimeSearched, dsl)

	fun findById(
		projectId: UUID,
		id: UUID,
		visibilitySearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
		using: DSLContext
	): Mono<ParticipantEntity> {
		val user = userTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		return Mono.from(
			fullSelect(projectId, dateTimeSearched, user, project, creator, editor, using = using)
				.where(
					TB_PARTICIPANT.PURGED.isFalse
						.and(TB_PARTICIPANT.PROJECT_ID.eq(projectId))
						.and(TB_PARTICIPANT.ID.eq(id))
						.and(visibleCondition(TB_PARTICIPANT.VISIBLE, visibilitySearched))
				)
		).map { it.toEntity(user, project, creator, editor, includeGroups = true, includeLastMovement = true) }
	}

	fun updateAllEndAvailability(
		ids: List<UUID>,
		endAvailabilityTime: OffsetTime?,
		endAvailabilityDate: LocalDate?
	): Flux<ParticipantEntity> {
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

	fun save(entity: ParticipantEntity, using: DSLContext): Mono<ParticipantEntity> =
		if (entity.id == null) insert(using, entity) else update(using, entity)

	fun saveAll(entities: Iterable<ParticipantEntity>): Flux<ParticipantEntity> =
		Flux.fromIterable(entities).flatMap { save(it) }

	private fun insert(using: DSLContext, entity: ParticipantEntity): Mono<ParticipantEntity> {
		val record = using.newRecord(TB_PARTICIPANT)
		record.setGeneric(entity, columns)
		record.setGenericProject(entity, TB_PARTICIPANT.PROJECT_ID)
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
			.setGeneric(entity, columns)
			.setGenericProject(entity, TB_PARTICIPANT.PROJECT_ID)
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

	fun deleteById(id: UUID): Mono<Unit> =
		Mono.from(dsl.deleteFrom(TB_PARTICIPANT).where(TB_PARTICIPANT.ID.eq(id))).map { }

	fun deleteAllById(ids: List<UUID>): Mono<Unit> =
		Mono.from(dsl.deleteFrom(TB_PARTICIPANT).where(TB_PARTICIPANT.ID.`in`(ids))).map { }

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
		fillGeneric(this, columns, creator, editor, fullCount)
		fillGenericProject(this, TB_PARTICIPANT.PROJECT_ID, project)
	}
}
