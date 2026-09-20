package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.activity.ActivityEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbProject
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbUser
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_ACTIVITY
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.routines.references.similarity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.creatorTable
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.editorTable
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.projectTable
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.visibleCondition
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.max
import org.jooq.impl.DSL.name
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
class ActivityJooqRepository(private val dsl: DSLContext) {
	private fun searchConditions(
		projectId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
	): Condition {
		val conditions = mutableListOf(TB_ACTIVITY.PROJECT_ID.eq(projectId))
		textSearched?.let { conditions += similarity(TB_ACTIVITY.SEARCH_TEXT, DSL.`val`(it)).gt(0f) }
		conditions += visibleCondition(TB_ACTIVITY.VISIBLE, visibilitySearched)
		availabilitySearched?.let { conditions += availabilityCondition(it) }
		dateTimeSearched?.let { conditions += dateInRangeCondition(it) }
		return DSL.and(conditions)
	}

	private fun availabilityCondition(availabilitySearched: Boolean): Condition {
		val startsBeforeNow = TB_ACTIVITY.START_AVAILABILITY_DATE.isNull
			.or(TB_ACTIVITY.START_AVAILABILITY_DATE.lt(DSL.currentLocalDate()))
			.or(
				TB_ACTIVITY.START_AVAILABILITY_DATE.eq(DSL.currentLocalDate())
					.and(TB_ACTIVITY.START_AVAILABILITY_TIME.isNull.or(TB_ACTIVITY.START_AVAILABILITY_TIME.le(DSL.currentOffsetTime())))
			)
		val endsAfterNow = TB_ACTIVITY.END_AVAILABILITY_DATE.isNull
			.or(TB_ACTIVITY.END_AVAILABILITY_DATE.gt(DSL.currentLocalDate()))
			.or(
				TB_ACTIVITY.END_AVAILABILITY_DATE.eq(DSL.currentLocalDate())
					.and(TB_ACTIVITY.END_AVAILABILITY_TIME.isNull.or(TB_ACTIVITY.END_AVAILABILITY_TIME.ge(DSL.currentOffsetTime())))
			)
		val isAvailable = startsBeforeNow.and(endsAfterNow)
		return if (availabilitySearched) isAvailable else isAvailable.not()
	}

	private fun dateInRangeCondition(dateTimeSearched: ZonedDateTime): Condition {
		val date = dateTimeSearched.toLocalDate()
		val time = dateTimeSearched.toOffsetDateTime().toOffsetTime()
		val startsBefore = TB_ACTIVITY.START_AVAILABILITY_DATE.isNull
			.or(TB_ACTIVITY.START_AVAILABILITY_DATE.lt(date))
			.or(TB_ACTIVITY.START_AVAILABILITY_DATE.eq(date).and(TB_ACTIVITY.START_AVAILABILITY_TIME.isNull.or(TB_ACTIVITY.START_AVAILABILITY_TIME.le(time))))
		val endsAfter = TB_ACTIVITY.END_AVAILABILITY_DATE.isNull
			.or(TB_ACTIVITY.END_AVAILABILITY_DATE.gt(date))
			.or(TB_ACTIVITY.END_AVAILABILITY_DATE.eq(date).and(TB_ACTIVITY.END_AVAILABILITY_TIME.isNull.or(TB_ACTIVITY.END_AVAILABILITY_TIME.ge(time))))
		return startsBefore.and(endsAfter)
	}

	fun findAll(
		projectId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
		limit: Int,
		offset: Int,
	): Flux<ActivityEntity> {
		val creator = creatorTable()
		val editor = editorTable()
		val project = projectTable()
		val similarityScore = (if (textSearched == null) DSL.inline(1f) else similarity(TB_ACTIVITY.SEARCH_TEXT, DSL.`val`(textSearched))).`as`("similarity_score")
		val fullCount = count().over().`as`("full_count")
		return Flux.from(
			dsl.select(
				TB_ACTIVITY.asterisk(), similarityScore,
				project.NAME, project.BEGIN_DATE, project.BEGIN_TIME, project.END_DATE, project.END_TIME, project.OPTIONS,
				creator.FIRST_NAME, creator.LAST_NAME, creator.EMAIL, editor.FIRST_NAME, editor.LAST_NAME, editor.EMAIL,
				fullCount,
			)
				.from(TB_ACTIVITY)
				.join(project).on(TB_ACTIVITY.PROJECT_ID.eq(project.ID).and(project.VISIBLE.isTrue))
				.leftJoin(creator).on(TB_ACTIVITY.CREATED_BY.eq(creator.ID))
				.leftJoin(editor).on(TB_ACTIVITY.LAST_MODIFIED_BY.eq(editor.ID))
				.where(searchConditions(projectId, textSearched, visibilitySearched, availabilitySearched, dateTimeSearched))
				.orderBy(similarityScore.desc(), TB_ACTIVITY.NAME)
				.limit(limit).offset(offset)
		).map { it.toEntity(creator, editor, project, fullCount) }
	}

	fun findAllByIds(projectId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<ActivityEntity> {
		if (ids.isEmpty()) return Flux.empty()
		val creator = creatorTable()
		val editor = editorTable()
		val project = projectTable()
		return Flux.from(
			dsl.select(
				TB_ACTIVITY.asterisk(),
				project.NAME, project.BEGIN_DATE, project.BEGIN_TIME, project.END_DATE, project.END_TIME, project.OPTIONS,
				creator.FIRST_NAME, creator.LAST_NAME, creator.EMAIL, editor.FIRST_NAME, editor.LAST_NAME, editor.EMAIL,
			)
				.from(TB_ACTIVITY)
				.join(project).on(TB_ACTIVITY.PROJECT_ID.eq(project.ID).and(project.VISIBLE.isTrue))
				.leftJoin(creator).on(TB_ACTIVITY.CREATED_BY.eq(creator.ID))
				.leftJoin(editor).on(TB_ACTIVITY.LAST_MODIFIED_BY.eq(editor.ID))
				.where(TB_ACTIVITY.PROJECT_ID.eq(projectId).and(TB_ACTIVITY.ID.`in`(ids)).and(visibleCondition(TB_ACTIVITY.VISIBLE, visibilitySearched)))
		).map { it.toEntity(creator, editor, project) }
	}

	fun findWithLimit(
		projectId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
		limit: Int,
	): Flux<ActivityEntity> {
		val creator = creatorTable()
		val editor = editorTable()
		val project = projectTable()
		val similarityScore = (if (textSearched == null) DSL.inline(1f) else similarity(TB_ACTIVITY.SEARCH_TEXT, DSL.`val`(textSearched))).`as`("similarity_score")
		return Flux.from(
			dsl.select(
				TB_ACTIVITY.asterisk(), similarityScore,
				project.NAME, project.BEGIN_DATE, project.BEGIN_TIME, project.END_DATE, project.END_TIME, project.OPTIONS,
				creator.FIRST_NAME, creator.LAST_NAME, creator.EMAIL, editor.FIRST_NAME, editor.LAST_NAME, editor.EMAIL,
			)
				.from(TB_ACTIVITY)
				.join(project).on(TB_ACTIVITY.PROJECT_ID.eq(project.ID).and(project.VISIBLE.isTrue))
				.leftJoin(creator).on(TB_ACTIVITY.CREATED_BY.eq(creator.ID))
				.leftJoin(editor).on(TB_ACTIVITY.LAST_MODIFIED_BY.eq(editor.ID))
				.where(searchConditions(projectId, textSearched, visibilitySearched, availabilitySearched, dateTimeSearched))
				.orderBy(similarityScore.desc(), TB_ACTIVITY.NAME)
				.limit(limit)
		).map { it.toEntity(creator, editor, project) }
	}

	fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<ActivityEntity> {
		val creator = creatorTable()
		val editor = editorTable()
		val project = projectTable()
		return Mono.from(
			dsl.select(
				TB_ACTIVITY.asterisk(),
				project.NAME, project.BEGIN_DATE, project.BEGIN_TIME, project.END_DATE, project.END_TIME, project.OPTIONS,
				creator.FIRST_NAME, creator.LAST_NAME, creator.EMAIL, editor.FIRST_NAME, editor.LAST_NAME, editor.EMAIL,
			)
				.from(TB_ACTIVITY)
				.join(project).on(TB_ACTIVITY.PROJECT_ID.eq(project.ID).and(project.VISIBLE.isTrue))
				.leftJoin(creator).on(TB_ACTIVITY.CREATED_BY.eq(creator.ID))
				.leftJoin(editor).on(TB_ACTIVITY.LAST_MODIFIED_BY.eq(editor.ID))
				.where(TB_ACTIVITY.PROJECT_ID.eq(projectId).and(TB_ACTIVITY.ID.eq(id)).and(visibleCondition(TB_ACTIVITY.VISIBLE, visibilitySearched)))
		).map { it.toEntity(creator, editor, project) }
	}

	fun findUnusedSince(dateThreshold: LocalDate): Flux<UUID> {
		val lastMovement = max(TB_MOVEMENT.DATE_TIME).`as`("max")
		val lu = dsl.select(lastMovement, TB_MOVEMENT.ACTIVITY_ID)
			.from(TB_MOVEMENT)
			.where(TB_MOVEMENT.ACTIVITY_ID.isNotNull)
			.groupBy(TB_MOVEMENT.ACTIVITY_ID)
			.asTable("lu")
		val luMax = field(name("lu", "max"), ZonedDateTime::class.java)
		val luActivityId = field(name("lu", "activity_id"), UUID::class.java)
		val thresholdAsTimestamp = DSL.`val`(dateThreshold).cast(TB_ACTIVITY.LAST_MODIFIED_DATE)
		return Flux.from(
			dsl.select(TB_ACTIVITY.ID)
				.from(TB_ACTIVITY)
				.leftJoin(lu).on(luActivityId.eq(TB_ACTIVITY.ID))
				.where(luMax.isNull.or(luMax.lt(thresholdAsTimestamp)).and(TB_ACTIVITY.LAST_MODIFIED_DATE.lt(thresholdAsTimestamp)))
		).map { it.value1()!! }
	}

	fun save(entity: ActivityEntity): Mono<ActivityEntity> = if (entity.id == null) insert(entity) else update(entity)

	private fun insert(entity: ActivityEntity): Mono<ActivityEntity> {
		val record = dsl.newRecord(TB_ACTIVITY)
		entity.visible?.let { record.set(TB_ACTIVITY.VISIBLE, it) }
		entity.createdAt?.let { record.set(TB_ACTIVITY.CREATED_DATE, it) }
		entity.creatorId?.let { record.set(TB_ACTIVITY.CREATED_BY, it) }
		entity.lastUpdateAt?.let { record.set(TB_ACTIVITY.LAST_MODIFIED_DATE, it) }
		entity.lastEditorId?.let { record.set(TB_ACTIVITY.LAST_MODIFIED_BY, it) }
		entity.projectId?.let { record.set(TB_ACTIVITY.PROJECT_ID, it) }
		entity.name?.let { record.set(TB_ACTIVITY.NAME, it) }
		entity.description?.let { record.set(TB_ACTIVITY.DESCRIPTION, it) }
		entity.duration?.let { record.set(TB_ACTIVITY.DURATION, it) }
		entity.minAllowedParticipants?.let { record.set(TB_ACTIVITY.MIN_ALLOWED_PARTICIPANTS, it) }
		entity.maxAllowedParticipants?.let { record.set(TB_ACTIVITY.MAX_ALLOWED_PARTICIPANTS, it) }
		entity.startAvailabilityDate?.let { record.set(TB_ACTIVITY.START_AVAILABILITY_DATE, it) }
		entity.startAvailabilityTime?.let { record.set(TB_ACTIVITY.START_AVAILABILITY_TIME, it) }
		entity.endAvailabilityDate?.let { record.set(TB_ACTIVITY.END_AVAILABILITY_DATE, it) }
		entity.endAvailabilityTime?.let { record.set(TB_ACTIVITY.END_AVAILABILITY_TIME, it) }
		return Mono.from(dsl.insertInto(TB_ACTIVITY).set(record).returning()).map { it.toEntity() }
	}

	private fun update(entity: ActivityEntity): Mono<ActivityEntity> = Mono.from(
		dsl.update(TB_ACTIVITY)
			.set(TB_ACTIVITY.VISIBLE, entity.visible)
			.set(TB_ACTIVITY.CREATED_DATE, entity.createdAt)
			.set(TB_ACTIVITY.CREATED_BY, entity.creatorId)
			.set(TB_ACTIVITY.LAST_MODIFIED_DATE, entity.lastUpdateAt)
			.set(TB_ACTIVITY.LAST_MODIFIED_BY, entity.lastEditorId)
			.set(TB_ACTIVITY.PROJECT_ID, entity.projectId)
			.set(TB_ACTIVITY.NAME, entity.name)
			.set(TB_ACTIVITY.DESCRIPTION, entity.description)
			.set(TB_ACTIVITY.DURATION, entity.duration)
			.set(TB_ACTIVITY.MIN_ALLOWED_PARTICIPANTS, entity.minAllowedParticipants)
			.set(TB_ACTIVITY.MAX_ALLOWED_PARTICIPANTS, entity.maxAllowedParticipants)
			.set(TB_ACTIVITY.START_AVAILABILITY_DATE, entity.startAvailabilityDate)
			.set(TB_ACTIVITY.START_AVAILABILITY_TIME, entity.startAvailabilityTime)
			.set(TB_ACTIVITY.END_AVAILABILITY_DATE, entity.endAvailabilityDate)
			.set(TB_ACTIVITY.END_AVAILABILITY_TIME, entity.endAvailabilityTime)
			.where(TB_ACTIVITY.ID.eq(entity.id))
			.returning()
	).map { it.toEntity() }

	fun deleteById(id: UUID): Mono<Unit> = Mono.from(dsl.deleteFrom(TB_ACTIVITY).where(TB_ACTIVITY.ID.eq(id))).map { }

	private fun Record.toEntity(
		creator: TbUser? = null,
		editor: TbUser? = null,
		project: TbProject? = null,
		fullCount: org.jooq.Field<Int>? = null,
	): ActivityEntity = ActivityEntity(
		name = get(TB_ACTIVITY.NAME),
		description = get(TB_ACTIVITY.DESCRIPTION),
		duration = get(TB_ACTIVITY.DURATION),
		minAllowedParticipants = get(TB_ACTIVITY.MIN_ALLOWED_PARTICIPANTS),
		maxAllowedParticipants = get(TB_ACTIVITY.MAX_ALLOWED_PARTICIPANTS),
		startAvailabilityDate = get(TB_ACTIVITY.START_AVAILABILITY_DATE),
		startAvailabilityTime = get(TB_ACTIVITY.START_AVAILABILITY_TIME),
		endAvailabilityDate = get(TB_ACTIVITY.END_AVAILABILITY_DATE),
		endAvailabilityTime = get(TB_ACTIVITY.END_AVAILABILITY_TIME),
	).apply {
		id = get(TB_ACTIVITY.ID)
		visible = get(TB_ACTIVITY.VISIBLE)
		createdAt = get(TB_ACTIVITY.CREATED_DATE)
		creatorId = get(TB_ACTIVITY.CREATED_BY)
		creatorFirstName = creator?.let { get(it.FIRST_NAME) }
		creatorLastName = creator?.let { get(it.LAST_NAME) }
		creatorEmail = creator?.let { get(it.EMAIL) }
		lastUpdateAt = get(TB_ACTIVITY.LAST_MODIFIED_DATE)
		lastEditorId = get(TB_ACTIVITY.LAST_MODIFIED_BY)
		lastEditorFirstName = editor?.let { get(it.FIRST_NAME) }
		lastEditorLastName = editor?.let { get(it.LAST_NAME) }
		lastEditorEmail = editor?.let { get(it.EMAIL) }
		this.fullCount = fullCount?.let { get(it)?.toLong() }
		projectId = get(TB_ACTIVITY.PROJECT_ID)
		projectName = project?.let { get(it.NAME) }
		projectStartDate = project?.let { get(it.BEGIN_DATE) }
		projectStartTime = project?.let { get(it.BEGIN_TIME) }
		projectEndDate = project?.let { get(it.END_DATE) }
		projectEndTime = project?.let { get(it.END_TIME) }
		projectOptions = project?.let { get(it.OPTIONS) }
	}
}
