package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.routines.references.similarity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbActivity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbProject
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbUser
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_ACTIVITY
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_COMMUNICATION
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
import org.jooq.Record
import org.jooq.SelectField
import org.jooq.SelectOnConditionStep
import org.jooq.impl.DSL
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.max
import org.jooq.impl.DSL.name
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.UUID

@Repository
class MovementJooqRepository(private val dsl: DSLContext) {
	private val columns = GenericColumns(
		TB_MOVEMENT.ID,
		TB_MOVEMENT.VISIBLE,
		TB_MOVEMENT.CREATED_DATE,
		TB_MOVEMENT.CREATED_BY,
		TB_MOVEMENT.LAST_MODIFIED_DATE,
		TB_MOVEMENT.LAST_MODIFIED_BY
	)

	private fun activityTable(): TbActivity = TB_ACTIVITY.`as`("activity_tb")

	private fun lastMovementPerParticipant() = dsl.select(
		max(TB_MOVEMENT.DATE_TIME).`as`("participant_last_movement_date_time"),
		TB_MOVEMENT_CONTENT.PARTICIPANT_ID,
	)
		.from(TB_MOVEMENT)
		.join(TB_MOVEMENT_CONTENT).on(TB_MOVEMENT_CONTENT.MOVEMENT_ID.eq(TB_MOVEMENT.ID))
		.where(TB_MOVEMENT.VISIBLE.isTrue)
		.groupBy(TB_MOVEMENT_CONTENT.PARTICIPANT_ID)
		.asTable("plm")

	private fun lastParticipantMovementCte(): CommonTableExpression<*> {
		val plm = lastMovementPerParticipant()
		val plmDateTime = field(name("plm", "participant_last_movement_date_time"), ZonedDateTime::class.java)
		val plmParticipantId = field(name("plm", "participant_id"), UUID::class.java)
		return name("last_participant_movement").`as`(
			dsl.select(TB_MOVEMENT.ID, TB_MOVEMENT.TYPE.`as`("type"), plmParticipantId.`as`("participant_id"))
				.from(TB_MOVEMENT)
				.join(plm).on(plmDateTime.eq(TB_MOVEMENT.DATE_TIME))
		)
	}

	private fun filteredGroupsCte(projectId: UUID): CommonTableExpression<*> {
		val groupPresence = TB_GROUP.`as`("group_presence")
		return name("filtered_groups").`as`(
			dsl.select(
				TB_PARTICIPANT.ID.`as`("participant_id"),
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
			)
				.from(TB_PARTICIPANT)
				.leftJoin(TB_GROUP_CONTENT).on(TB_GROUP_CONTENT.PARTICIPANT_ID.eq(TB_PARTICIPANT.ID))
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
				.where(TB_PARTICIPANT.PROJECT_ID.eq(projectId))
				.groupBy(TB_PARTICIPANT.ID)
		)
	}

	private fun currentMovementCtes(projectId: UUID): Triple<CommonTableExpression<*>, CommonTableExpression<*>, CommonTableExpression<*>> {
		val lastParticipantMovement = lastParticipantMovementCte()
		val filteredGroups = filteredGroupsCte(projectId)
		val lpmId = field(name("last_participant_movement", "id"), UUID::class.java)
		val lpmType = field(name("last_participant_movement", "type"), String::class.java)
		val lpmParticipantId = field(name("last_participant_movement", "participant_id"), UUID::class.java)
		val fgParticipantId = field(name("filtered_groups", "participant_id"), UUID::class.java)
		val fgMinStart = field(name("filtered_groups", "min_start_availability"), OffsetDateTime::class.java)
		val fgMaxEnd = field(name("filtered_groups", "max_end_availability"), OffsetDateTime::class.java)

		val typeMatchesLastMovement = DSL.condition(
			"{0} = (CASE WHEN {1} = 'REGISTERED' THEN 'OUT' ELSE 'IN' END)",
			lpmType, TB_PARTICIPANT.TYPE,
		)
		val availability = DSL.condition(
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
			fgMinStart, fgMaxEnd,
		)

		val currentMovement = name("current_movement").`as`(
			dsl.select(lpmId.`as`("id"), lpmParticipantId.`as`("participant_id"))
				.from(TB_PARTICIPANT)
				.leftJoin(lastParticipantMovement).on(lpmParticipantId.eq(TB_PARTICIPANT.ID))
				.leftJoin(filteredGroups).on(fgParticipantId.eq(TB_PARTICIPANT.ID))
				.where(
					TB_PARTICIPANT.VISIBLE.isTrue
						.and(TB_PARTICIPANT.PURGED.isFalse)
						.and(TB_PARTICIPANT.PROJECT_ID.eq(projectId))
						.and(typeMatchesLastMovement)
						.and(availability)
				)
		)
		return Triple(lastParticipantMovement, filteredGroups, currentMovement)
	}

	private fun dateRangeOverlap(
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?
	): Condition {
		val afterStart = startDateTimeSearched?.let { TB_MOVEMENT.DATE_TIME.ge(it) } ?: DSL.noCondition()
		val beforeEnd = endDateTimeSearched?.let { TB_MOVEMENT.DATE_TIME.le(it) } ?: DSL.noCondition()
		return afterStart.and(beforeEnd)
	}

	private fun searchConditions(
		projectId: UUID,
		visibilitySearched: Boolean?,
		linkedToActivity: Boolean?,
		typeSearched: List<MovementTypeEnum>,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?,
	): Condition {
		val conditions = mutableListOf(
			TB_MOVEMENT.PROJECT_ID.eq(projectId),
			visibleCondition(TB_MOVEMENT.VISIBLE, visibilitySearched),
			TB_MOVEMENT.TYPE.`in`(typeSearched),
			dateRangeOverlap(startDateTimeSearched, endDateTimeSearched),
		)
		linkedToActivity?.let {
			val hasActivity = TB_MOVEMENT.ACTIVITY_ID.isNotNull
			conditions += if (it) hasActivity else hasActivity.not()
		}
		return DSL.and(conditions)
	}

	private fun baseSelect(
		activity: TbActivity,
		project: TbProject,
		creator: TbUser,
		editor: TbUser,
		vararg extraFields: SelectField<*>,
		using: DSLContext = dsl
	): SelectOnConditionStep<Record> =
		using.select(
			listOf(
				TB_MOVEMENT.asterisk(),
				activity.ID,
				activity.NAME,
				activity.DESCRIPTION,
				activity.DURATION,
				activity.MIN_ALLOWED_PARTICIPANTS,
				activity.MAX_ALLOWED_PARTICIPANTS,
				activity.START_AVAILABILITY_DATE,
				activity.START_AVAILABILITY_TIME,
				activity.END_AVAILABILITY_DATE,
				activity.END_AVAILABILITY_TIME,
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
			.from(TB_MOVEMENT)
			.leftJoin(activity).on(TB_MOVEMENT.ACTIVITY_ID.eq(activity.ID))
			.join(project).on(TB_MOVEMENT.PROJECT_ID.eq(project.ID).and(project.VISIBLE.isTrue))
			.leftJoin(creator).on(TB_MOVEMENT.CREATED_BY.eq(creator.ID))
			.leftJoin(editor).on(TB_MOVEMENT.LAST_MODIFIED_BY.eq(editor.ID))

	fun findAll(
		projectId: UUID,
		visibilitySearched: Boolean?,
		linkedToActivity: Boolean?,
		typeSearched: List<MovementTypeEnum>,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?,
		limit: Int,
		offset: Int,
	): Flux<MovementEntity> {
		val activity = activityTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		val fullCount = count().over().`as`("full_count")
		return Flux.from(
			baseSelect(activity, project, creator, editor, fullCount)
				.where(
					searchConditions(
						projectId,
						visibilitySearched,
						linkedToActivity,
						typeSearched,
						startDateTimeSearched,
						endDateTimeSearched
					)
				)
				.orderBy(TB_MOVEMENT.DATE_TIME.desc())
				.limit(limit).offset(offset)
		).map { it.toEntity(activity, project, creator, editor, fullCount) }
	}

	private fun lastMovementPerActivityCte(): CommonTableExpression<*> {
		val plmDateTime = field(name("plm", "last_movement_date_time"), ZonedDateTime::class.java)
		val plmActivityId = field(name("plm", "activity_id"), UUID::class.java)
		return name("last_activity_movement").`as`(
			dsl.select(TB_MOVEMENT.ID, plmActivityId)
				.from(TB_MOVEMENT)
				.join(
					dsl.select(
						max(TB_MOVEMENT.DATE_TIME).`as`("last_movement_date_time"),
						TB_MOVEMENT.ACTIVITY_ID,
					)
						.from(TB_MOVEMENT)
						.where(TB_MOVEMENT.VISIBLE.isTrue.and(TB_MOVEMENT.ACTIVITY_ID.isNotNull))
						.groupBy(TB_MOVEMENT.ACTIVITY_ID)
						.asTable("plm")
				).on(plmDateTime.eq(TB_MOVEMENT.DATE_TIME).and(plmActivityId.eq(TB_MOVEMENT.ACTIVITY_ID)))
		)
	}

	fun findOngoingActivityOutings(projectId: UUID, limit: Int): Flux<MovementEntity> {
		val activity = activityTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		val lastActivityMovement = lastMovementPerActivityCte()
		val lastActivityMovementId = field(name("last_activity_movement", "id"), UUID::class.java)
		return Flux.from(
			dsl.with(lastActivityMovement)
				.select(
					listOf(
						TB_MOVEMENT.asterisk(),
						activity.ID,
						activity.NAME,
						activity.DESCRIPTION,
						activity.DURATION,
						activity.MIN_ALLOWED_PARTICIPANTS,
						activity.MAX_ALLOWED_PARTICIPANTS,
						activity.START_AVAILABILITY_DATE,
						activity.START_AVAILABILITY_TIME,
						activity.END_AVAILABILITY_DATE,
						activity.END_AVAILABILITY_TIME,
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
				.from(TB_MOVEMENT)
				.join(lastActivityMovement).on(lastActivityMovementId.eq(TB_MOVEMENT.ID))
				.join(activity).on(TB_MOVEMENT.ACTIVITY_ID.eq(activity.ID))
				.join(project).on(TB_MOVEMENT.PROJECT_ID.eq(project.ID).and(project.VISIBLE.isTrue))
				.leftJoin(creator).on(TB_MOVEMENT.CREATED_BY.eq(creator.ID))
				.leftJoin(editor).on(TB_MOVEMENT.LAST_MODIFIED_BY.eq(editor.ID))
				.where(
					TB_MOVEMENT.PROJECT_ID.eq(projectId)
						.and(TB_MOVEMENT.TYPE.eq(MovementTypeEnum.OUT))
						.and(TB_MOVEMENT.VISIBLE.isTrue)
				)
				.orderBy(TB_MOVEMENT.DATE_TIME.desc())
				.limit(limit)
		).map { it.toEntity(activity, project, creator, editor) }
	}

	fun findAllByCreatorId(userId: UUID): Flux<MovementEntity> {
		val activity = activityTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		return Flux.from(
			baseSelect(activity, project, creator, editor)
				.where(TB_MOVEMENT.CREATED_BY.eq(userId))
				.orderBy(TB_MOVEMENT.DATE_TIME.desc())
		).map { it.toEntity(activity, project, creator, editor) }
	}

	fun findCurrent(
		projectId: UUID,
		visibilitySearched: Boolean?,
		linkedToActivity: Boolean?,
		typeSearched: List<MovementTypeEnum>,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?,
		limit: Int,
		offset: Int,
	): Flux<MovementEntity> {
		val activity = activityTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		val (lastParticipantMovement, filteredGroups, currentMovement) = currentMovementCtes(projectId)
		val cmId = field(name("current_movement", "id"), UUID::class.java)
		return Flux.from(
			dsl.with(lastParticipantMovement, filteredGroups, currentMovement)
				.selectDistinct(
					listOf(
						TB_MOVEMENT.asterisk(),
						activity.ID,
						activity.NAME,
						activity.DESCRIPTION,
						activity.DURATION,
						activity.MIN_ALLOWED_PARTICIPANTS,
						activity.MAX_ALLOWED_PARTICIPANTS,
						activity.START_AVAILABILITY_DATE,
						activity.START_AVAILABILITY_TIME,
						activity.END_AVAILABILITY_DATE,
						activity.END_AVAILABILITY_TIME,
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
				.from(TB_MOVEMENT)
				.join(currentMovement).on(cmId.eq(TB_MOVEMENT.ID))
				.leftJoin(activity).on(TB_MOVEMENT.ACTIVITY_ID.eq(activity.ID))
				.join(project).on(TB_MOVEMENT.PROJECT_ID.eq(project.ID).and(project.VISIBLE.isTrue))
				.leftJoin(creator).on(TB_MOVEMENT.CREATED_BY.eq(creator.ID))
				.leftJoin(editor).on(TB_MOVEMENT.LAST_MODIFIED_BY.eq(editor.ID))
				.where(
					searchConditions(
						projectId,
						visibilitySearched,
						linkedToActivity,
						typeSearched,
						startDateTimeSearched,
						endDateTimeSearched
					)
				)
				.orderBy(TB_MOVEMENT.DATE_TIME.desc())
				.limit(limit).offset(offset)
		).map { it.toEntity(activity, project, creator, editor) }
	}

	fun countCurrent(
		projectId: UUID,
		visibilitySearched: Boolean?,
		linkedToActivity: Boolean?,
		typeSearched: List<MovementTypeEnum>,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?,
	): Mono<Long> {
		val (lastParticipantMovement, filteredGroups, currentMovement) = currentMovementCtes(projectId)
		val cmId = field(name("current_movement", "id"), UUID::class.java)
		return Mono.from(
			dsl.with(lastParticipantMovement, filteredGroups, currentMovement)
				.select(DSL.countDistinct(TB_MOVEMENT.ID))
				.from(TB_MOVEMENT)
				.join(currentMovement).on(cmId.eq(TB_MOVEMENT.ID))
				.where(
					searchConditions(
						projectId,
						visibilitySearched,
						linkedToActivity,
						typeSearched,
						startDateTimeSearched,
						endDateTimeSearched
					)
				)
		).map { it.value1()!!.toLong() }
	}

	fun findAllByParticipantId(
		projectId: UUID,
		participantId: UUID,
		visibilitySearched: Boolean?,
		linkedToActivity: Boolean?,
		typeSearched: List<MovementTypeEnum>,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?,
		limit: Int,
		offset: Int,
	): Flux<MovementEntity> {
		val activity = activityTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		val fullCount = count().over().`as`("full_count")
		return Flux.from(
			baseSelect(activity, project, creator, editor, fullCount)
				.join(TB_MOVEMENT_CONTENT).on(
					TB_MOVEMENT_CONTENT.MOVEMENT_ID.eq(TB_MOVEMENT.ID)
						.and(TB_MOVEMENT_CONTENT.PARTICIPANT_ID.eq(participantId))
				)
				.where(
					searchConditions(
						projectId,
						visibilitySearched,
						linkedToActivity,
						typeSearched,
						startDateTimeSearched,
						endDateTimeSearched
					)
				)
				.orderBy(TB_MOVEMENT.DATE_TIME.desc())
				.limit(limit).offset(offset)
		).map { it.toEntity(activity, project, creator, editor, fullCount) }
	}

	fun countAllByParticipantId(
		projectId: UUID,
		participantId: UUID,
		visibilitySearched: Boolean?,
		linkedToActivity: Boolean?,
		typeSearched: List<MovementTypeEnum>,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?,
	): Mono<Long> = Mono.from(
		dsl.select(count(TB_MOVEMENT.ID))
			.from(TB_MOVEMENT_CONTENT)
			.join(TB_MOVEMENT).on(TB_MOVEMENT_CONTENT.MOVEMENT_ID.eq(TB_MOVEMENT.ID))
			.where(
				TB_MOVEMENT_CONTENT.PARTICIPANT_ID.eq(participantId).and(
					searchConditions(
						projectId,
						visibilitySearched,
						linkedToActivity,
						typeSearched,
						startDateTimeSearched,
						endDateTimeSearched
					)
				)
			)
	).map { it.value1()!!.toLong() }

	fun findAllByVehicleId(
		projectId: UUID,
		vehicleId: UUID,
		visibilitySearched: Boolean?,
		linkedToActivity: Boolean?,
		typeSearched: List<MovementTypeEnum>,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?,
		limit: Int,
		offset: Int,
	): Flux<MovementEntity> {
		val activity = activityTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		val fullCount = count().over().`as`("full_count")
		return Flux.from(
			baseSelect(activity, project, creator, editor, fullCount)
				.join(TB_MOVEMENT_CONTENT).on(
					TB_MOVEMENT_CONTENT.MOVEMENT_ID.eq(TB_MOVEMENT.ID).and(TB_MOVEMENT_CONTENT.VEHICLE_ID.eq(vehicleId))
				)
				.where(
					searchConditions(
						projectId,
						visibilitySearched,
						linkedToActivity,
						typeSearched,
						startDateTimeSearched,
						endDateTimeSearched
					)
				)
				.orderBy(TB_MOVEMENT.DATE_TIME.desc())
				.limit(limit).offset(offset)
		).map { it.toEntity(activity, project, creator, editor, fullCount) }
	}

	fun countAllByVehicleId(
		projectId: UUID,
		vehicleId: UUID,
		visibilitySearched: Boolean?,
		linkedToActivity: Boolean?,
		typeSearched: List<MovementTypeEnum>,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?,
	): Mono<Long> = Mono.from(
		dsl.select(count(TB_MOVEMENT.ID))
			.from(TB_MOVEMENT_CONTENT)
			.join(TB_MOVEMENT).on(TB_MOVEMENT_CONTENT.MOVEMENT_ID.eq(TB_MOVEMENT.ID))
			.where(
				TB_MOVEMENT_CONTENT.VEHICLE_ID.eq(vehicleId).and(
					searchConditions(
						projectId,
						visibilitySearched,
						linkedToActivity,
						typeSearched,
						startDateTimeSearched,
						endDateTimeSearched
					)
				)
			)
	).map { it.value1()!!.toLong() }

	fun findAllByActivityId(
		projectId: UUID,
		activityId: UUID,
		visibilitySearched: Boolean?,
		typeSearched: List<MovementTypeEnum>,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?,
		limit: Int,
		offset: Int,
	): Flux<MovementEntity> {
		val activity = activityTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		val fullCount = count().over().`as`("full_count")
		return Flux.from(
			baseSelect(activity, project, creator, editor, fullCount)
				.where(
					TB_MOVEMENT.PROJECT_ID.eq(projectId)
						.and(TB_MOVEMENT.ACTIVITY_ID.eq(activityId))
						.and(visibleCondition(TB_MOVEMENT.VISIBLE, visibilitySearched))
						.and(TB_MOVEMENT.TYPE.`in`(typeSearched))
						.and(dateRangeOverlap(startDateTimeSearched, endDateTimeSearched))
				)
				.orderBy(TB_MOVEMENT.DATE_TIME.desc())
				.limit(limit).offset(offset)
		).map { it.toEntity(activity, project, creator, editor, fullCount) }
	}

	fun countAllByActivityId(
		projectId: UUID,
		activityId: UUID,
		visibilitySearched: Boolean?,
		typeSearched: List<MovementTypeEnum>,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?,
	): Mono<Long> = Mono.from(
		dsl.select(count(TB_MOVEMENT.ID))
			.from(TB_MOVEMENT)
			.where(
				TB_MOVEMENT.PROJECT_ID.eq(projectId)
					.and(TB_MOVEMENT.ACTIVITY_ID.eq(activityId))
					.and(visibleCondition(TB_MOVEMENT.VISIBLE, visibilitySearched))
					.and(TB_MOVEMENT.TYPE.`in`(typeSearched))
					.and(dateRangeOverlap(startDateTimeSearched, endDateTimeSearched))
			)
	).map { it.value1()!!.toLong() }

	fun findByActivityWithLimit(
		projectId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
		limit: Int,
	): Flux<MovementEntity> {
		val activity = activityTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		val plm = lastMovementPerParticipant()
		val plmDateTime = field(name("plm", "participant_last_movement_date_time"), ZonedDateTime::class.java)
		val similarityScore = (if (textSearched == null) DSL.inline(1f) else similarity(
			activity.SEARCH_TEXT,
			DSL.`val`(textSearched)
		)).`as`("similarity_score")

		val conditions = mutableListOf(
			TB_MOVEMENT.PROJECT_ID.eq(projectId),
			TB_MOVEMENT.ACTIVITY_ID.isNotNull.and(textSearched?.let {
				similarity(
					activity.SEARCH_TEXT,
					DSL.`val`(it)
				).gt(0f)
			} ?: DSL.noCondition()),
			visibleCondition(TB_MOVEMENT.VISIBLE, visibilitySearched),
		)
		availabilitySearched?.let {
			val cond = activeNowCondition(
				activity.START_AVAILABILITY_DATE,
				activity.START_AVAILABILITY_TIME,
				activity.END_AVAILABILITY_DATE,
				activity.END_AVAILABILITY_TIME
			)
			conditions += if (it) cond else cond.not()
		}
		dateTimeSearched?.let {
			conditions += activeAtCondition(
				activity.START_AVAILABILITY_DATE,
				activity.START_AVAILABILITY_TIME,
				activity.END_AVAILABILITY_DATE,
				activity.END_AVAILABILITY_TIME,
				it
			)
		}

		return Flux.from(
			dsl.select(
				listOf(
					TB_MOVEMENT.asterisk(),
					similarityScore,
					activity.ID,
					activity.NAME,
					activity.DESCRIPTION,
					activity.DURATION,
					activity.MIN_ALLOWED_PARTICIPANTS,
					activity.MAX_ALLOWED_PARTICIPANTS,
					activity.START_AVAILABILITY_DATE,
					activity.START_AVAILABILITY_TIME,
					activity.END_AVAILABILITY_DATE,
					activity.END_AVAILABILITY_TIME,
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
				.from(TB_MOVEMENT)
				.join(plm).on(plmDateTime.eq(TB_MOVEMENT.DATE_TIME))
				.leftJoin(activity).on(TB_MOVEMENT.ACTIVITY_ID.eq(activity.ID))
				.join(project).on(TB_MOVEMENT.PROJECT_ID.eq(project.ID).and(project.VISIBLE.isTrue))
				.leftJoin(creator).on(TB_MOVEMENT.CREATED_BY.eq(creator.ID))
				.leftJoin(editor).on(TB_MOVEMENT.LAST_MODIFIED_BY.eq(editor.ID))
				.where(DSL.and(conditions))
				.groupBy(
					TB_MOVEMENT.ID,
					TB_MOVEMENT.DATE_TIME,
					TB_MOVEMENT.TYPE,
					TB_MOVEMENT.ACTIVITY_ID,
					TB_MOVEMENT.REASON,
					activity.SEARCH_TEXT,
					project.ID,
					project.NAME,
					project.BEGIN_DATE,
					project.BEGIN_TIME,
					project.END_DATE,
					project.END_TIME,
					project.OPTIONS,
					activity.ID,
					activity.NAME,
					activity.DESCRIPTION,
					activity.DURATION,
					activity.MIN_ALLOWED_PARTICIPANTS,
					activity.MAX_ALLOWED_PARTICIPANTS,
					activity.START_AVAILABILITY_DATE,
					activity.START_AVAILABILITY_TIME,
					activity.END_AVAILABILITY_DATE,
					activity.END_AVAILABILITY_TIME,
					creator.FIRST_NAME,
					creator.LAST_NAME,
					creator.EMAIL,
					editor.FIRST_NAME,
					editor.LAST_NAME,
					editor.EMAIL,
					TB_MOVEMENT.VISIBLE,
					TB_MOVEMENT.CREATED_BY,
					TB_MOVEMENT.CREATED_DATE,
					TB_MOVEMENT.LAST_MODIFIED_BY,
					TB_MOVEMENT.LAST_MODIFIED_DATE,
				)
				.orderBy(similarityScore.desc(), activity.NAME)
				.limit(limit)
		).map { it.toEntity(activity, project, creator, editor) }
	}

	fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<MovementEntity> =
		findById(projectId, id, visibilitySearched, dsl)

	fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?, using: DSLContext): Mono<MovementEntity> {
		val activity = activityTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		return Mono.from(
			baseSelect(activity, project, creator, editor, using = using)
				.where(
					TB_MOVEMENT.PROJECT_ID.eq(projectId).and(TB_MOVEMENT.ID.eq(id))
						.and(visibleCondition(TB_MOVEMENT.VISIBLE, visibilitySearched))
				)
		).map { it.toEntity(activity, project, creator, editor) }
	}

	fun findOlderThanAndUncommentedSince(dateThreshold: LocalDate): Flux<UUID> {
		val lc = dsl.select(max(TB_COMMUNICATION.DATE_TIME).`as`("max"), TB_COMMUNICATION.MOVEMENT_ID)
			.from(TB_COMMUNICATION)
			.where(TB_COMMUNICATION.MOVEMENT_ID.isNotNull)
			.groupBy(TB_COMMUNICATION.MOVEMENT_ID)
			.asTable("lc")
		val lcMax = field(name("lc", "max"), ZonedDateTime::class.java)
		val lcMovementId = field(name("lc", "movement_id"), UUID::class.java)
		return Flux.from(
			dsl.select(TB_MOVEMENT.ID)
				.from(TB_MOVEMENT)
				.leftJoin(lc).on(lcMovementId.eq(TB_MOVEMENT.ID))
				.where(
					lcMax.isNull.or(DSL.condition("{0} < {1}", lcMax, DSL.`val`(dateThreshold)))
						.and(DSL.condition("{0} < {1}", TB_MOVEMENT.LAST_MODIFIED_DATE, DSL.`val`(dateThreshold)))
				)
		).map { it.value1()!! }
	}

	fun save(entity: MovementEntity): Mono<MovementEntity> = save(entity, dsl)

	fun save(entity: MovementEntity, using: DSLContext): Mono<MovementEntity> =
		if (entity.id == null) insert(using, entity) else update(using, entity)

	private fun insert(using: DSLContext, entity: MovementEntity): Mono<MovementEntity> {
		val record = using.newRecord(TB_MOVEMENT)
		record.setGeneric(entity, columns)
		record.setGenericProject(entity, TB_MOVEMENT.PROJECT_ID)
		entity.dateTime?.let { record.set(TB_MOVEMENT.DATE_TIME, it) }
		entity.type?.let { record.set(TB_MOVEMENT.TYPE, it) }
		entity.reason?.let { record.set(TB_MOVEMENT.REASON, it) }
		entity.activityId?.let { record.set(TB_MOVEMENT.ACTIVITY_ID, it) }
		return Mono.from(using.insertInto(TB_MOVEMENT).set(record).returning()).map { it.toEntity() }
	}

	private fun update(using: DSLContext, entity: MovementEntity): Mono<MovementEntity> = Mono.from(
		using.update(TB_MOVEMENT)
			.setGeneric(entity, columns)
			.setGenericProject(entity, TB_MOVEMENT.PROJECT_ID)
			.set(TB_MOVEMENT.DATE_TIME, entity.dateTime)
			.set(TB_MOVEMENT.TYPE, entity.type)
			.set(TB_MOVEMENT.REASON, entity.reason)
			.set(TB_MOVEMENT.ACTIVITY_ID, entity.activityId)
			.where(TB_MOVEMENT.ID.eq(entity.id))
			.returning()
	).map { it.toEntity() }

	fun deleteById(id: UUID): Mono<Unit> = Mono.from(dsl.deleteFrom(TB_MOVEMENT).where(TB_MOVEMENT.ID.eq(id))).map { }

	private fun Record.toEntity(
		activity: TbActivity? = null,
		project: TbProject? = null,
		creator: TbUser? = null,
		editor: TbUser? = null,
		fullCount: Field<Int>? = null,
	): MovementEntity = MovementEntity(
		dateTime = get(TB_MOVEMENT.DATE_TIME),
		type = get(TB_MOVEMENT.TYPE),
		reason = get(TB_MOVEMENT.REASON),
		activityId = get(TB_MOVEMENT.ACTIVITY_ID),
		activityName = activity?.let { get(it.NAME) },
		activityDescription = activity?.let { get(it.DESCRIPTION) },
		activityDuration = activity?.let { get(it.DURATION) },
		activityMinAllowedParticipants = activity?.let { get(it.MIN_ALLOWED_PARTICIPANTS) },
		activityMaxAllowedParticipants = activity?.let { get(it.MAX_ALLOWED_PARTICIPANTS) },
		activityStartAvailabilityDate = activity?.let { get(it.START_AVAILABILITY_DATE) },
		activityStartAvailabilityTime = activity?.let { get(it.START_AVAILABILITY_TIME) },
		activityEndAvailabilityDate = activity?.let { get(it.END_AVAILABILITY_DATE) },
		activityEndAvailabilityTime = activity?.let { get(it.END_AVAILABILITY_TIME) },
	).apply {
		fillGeneric(this, columns, creator, editor, fullCount)
		fillGenericProject(this, TB_MOVEMENT.PROJECT_ID, project)
	}
}
