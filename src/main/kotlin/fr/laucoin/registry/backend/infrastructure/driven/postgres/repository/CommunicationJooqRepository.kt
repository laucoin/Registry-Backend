package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.communication.CommunicationEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.routines.references.similarity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbActivity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbAlert
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbProject
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbUser
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_ACTIVITY
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_ALERT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_COMMUNICATION
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.GenericColumns
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.creatorTable
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.editorTable
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.fillGeneric
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.fillGenericProject
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.projectTable
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.setGeneric
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.setGenericProject
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.visibleCondition
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.SelectField
import org.jooq.SelectOnConditionStep
import org.jooq.impl.DSL
import org.jooq.impl.DSL.count
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.UUID

@Repository
class CommunicationJooqRepository(private val dsl: DSLContext) {
	private val columns = GenericColumns(TB_COMMUNICATION.ID, TB_COMMUNICATION.VISIBLE, TB_COMMUNICATION.CREATED_DATE, TB_COMMUNICATION.CREATED_BY, TB_COMMUNICATION.LAST_MODIFIED_DATE, TB_COMMUNICATION.LAST_MODIFIED_BY)

	private fun movementAlias() = TB_MOVEMENT.`as`("movement_tb")
	private fun activityAlias() = TB_ACTIVITY.`as`("activity_tb")
	private fun alertAlias() = TB_ALERT.`as`("alert_tb")

	private fun baseSelect(
		activity: TbActivity,
		alert: TbAlert,
		project: TbProject,
		creator: TbUser,
		editor: TbUser,
		vararg extraFields: SelectField<*>,
	): SelectOnConditionStep<Record> =
		dsl.select(
			listOf(
				TB_COMMUNICATION.asterisk(),
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
				alert.ID,
				alert.TITLE,
				alert.DATE_TIME,
				alert.STATUS,
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
			.from(TB_COMMUNICATION)
			.leftJoin(movementAlias()).on(TB_COMMUNICATION.MOVEMENT_ID.eq(movementAlias().ID))
			.leftJoin(activity).on(movementAlias().ACTIVITY_ID.eq(activity.ID))
			.leftJoin(alert).on(TB_COMMUNICATION.ALERT_ID.eq(alert.ID))
			.join(project).on(TB_COMMUNICATION.PROJECT_ID.eq(project.ID).and(project.VISIBLE.isTrue))
			.leftJoin(creator).on(TB_COMMUNICATION.CREATED_BY.eq(creator.ID))
			.leftJoin(editor).on(TB_COMMUNICATION.LAST_MODIFIED_BY.eq(editor.ID))

	private fun searchCondition(textSearched: String?): Condition =
		textSearched?.let { similarity(TB_COMMUNICATION.SEARCH_TEXT, DSL.`val`(it)).gt(0f) } ?: DSL.noCondition()

	private fun dateRangeCondition(
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?
	): Condition {
		val afterStart = startDateTimeSearched?.let { TB_COMMUNICATION.DATE_TIME.ge(it) } ?: DSL.noCondition()
		val beforeEnd = endDateTimeSearched?.let { TB_COMMUNICATION.DATE_TIME.le(it) } ?: DSL.noCondition()
		return afterStart.and(beforeEnd)
	}

	fun findAll(
		projectId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?,
		limit: Int,
		offset: Int,
	): Flux<CommunicationEntity> {
		val activity = activityAlias()
		val alert = alertAlias()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		val similarityScore = (if (textSearched == null) DSL.inline(1f) else similarity(
			TB_COMMUNICATION.SEARCH_TEXT,
			DSL.`val`(textSearched)
		)).`as`("similarity_score")
		val fullCount = count().over().`as`("full_count")
		return Flux.from(
			baseSelect(activity, alert, project, creator, editor, similarityScore, fullCount)
				.where(
					TB_COMMUNICATION.PROJECT_ID.eq(projectId)
						.and(visibleCondition(TB_COMMUNICATION.VISIBLE, visibilitySearched))
						.and(searchCondition(textSearched))
						.and(dateRangeCondition(startDateTimeSearched, endDateTimeSearched))
				)
				.orderBy(similarityScore.desc(), TB_COMMUNICATION.DATE_TIME.desc())
				.limit(limit).offset(offset)
		).map { it.toEntity(activity, alert, creator, editor, project, fullCount) }
	}

	fun findAllByMovementIdsWithLimit(
		projectId: UUID,
		movementIds: List<UUID>,
		visibilitySearched: Boolean?,
		limit: Int
	): Flux<CommunicationEntity> {
		val activity = activityAlias()
		val alert = alertAlias()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		return Flux.from(
			baseSelect(activity, alert, project, creator, editor)
				.where(
					TB_COMMUNICATION.PROJECT_ID.eq(projectId)
						.and(visibleCondition(TB_COMMUNICATION.VISIBLE, visibilitySearched))
						.and(TB_COMMUNICATION.MOVEMENT_ID.`in`(movementIds))
				)
				.orderBy(TB_COMMUNICATION.DATE_TIME.desc())
				.limit(limit)
		).map { it.toEntity(activity, alert, creator, editor, project) }
	}

	fun findAllByMovementId(
		projectId: UUID,
		movementId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?,
		limit: Int,
		offset: Int,
	): Flux<CommunicationEntity> {
		val activity = activityAlias()
		val alert = alertAlias()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		val similarityScore = (if (textSearched == null) DSL.inline(1f) else similarity(
			TB_COMMUNICATION.SEARCH_TEXT,
			DSL.`val`(textSearched)
		)).`as`("similarity_score")
		val fullCount = count().over().`as`("full_count")
		return Flux.from(
			baseSelect(activity, alert, project, creator, editor, similarityScore, fullCount)
				.where(
					TB_COMMUNICATION.PROJECT_ID.eq(projectId)
						.and(TB_COMMUNICATION.MOVEMENT_ID.eq(movementId))
						.and(visibleCondition(TB_COMMUNICATION.VISIBLE, visibilitySearched))
						.and(searchCondition(textSearched))
						.and(dateRangeCondition(startDateTimeSearched, endDateTimeSearched))
				)
				.orderBy(similarityScore.desc(), TB_COMMUNICATION.DATE_TIME.desc())
				.limit(limit).offset(offset)
		).map { it.toEntity(activity, alert, creator, editor, project, fullCount) }
	}

	fun countAllByMovementId(
		projectId: UUID,
		movementId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?,
	): Mono<Long> = Mono.from(
		dsl.select(count(TB_COMMUNICATION.ID))
			.from(TB_COMMUNICATION)
			.where(
				TB_COMMUNICATION.PROJECT_ID.eq(projectId)
					.and(TB_COMMUNICATION.MOVEMENT_ID.eq(movementId))
					.and(visibleCondition(TB_COMMUNICATION.VISIBLE, visibilitySearched))
					.and(searchCondition(textSearched))
					.and(dateRangeCondition(startDateTimeSearched, endDateTimeSearched))
			)
	).map { it.value1().toLong() }

	fun findAllByAlertIdsWithLimit(
		projectId: UUID,
		alertIds: List<UUID>,
		visibilitySearched: Boolean?,
		limit: Int
	): Flux<CommunicationEntity> {
		val activity = activityAlias()
		val alert = alertAlias()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		return Flux.from(
			baseSelect(activity, alert, project, creator, editor)
				.where(
					TB_COMMUNICATION.PROJECT_ID.eq(projectId)
						.and(visibleCondition(TB_COMMUNICATION.VISIBLE, visibilitySearched))
						.and(TB_COMMUNICATION.ALERT_ID.`in`(alertIds))
				)
				.orderBy(TB_COMMUNICATION.DATE_TIME.desc())
				.limit(limit)
		).map { it.toEntity(activity, alert, creator, editor, project) }
	}

	fun findAllByAlertId(
		projectId: UUID,
		alertId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?,
		limit: Int,
		offset: Int,
	): Flux<CommunicationEntity> {
		val activity = activityAlias()
		val alert = alertAlias()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		val similarityScore = (if (textSearched == null) DSL.inline(1f) else similarity(
			TB_COMMUNICATION.SEARCH_TEXT,
			DSL.`val`(textSearched)
		)).`as`("similarity_score")
		val fullCount = count().over().`as`("full_count")
		return Flux.from(
			baseSelect(activity, alert, project, creator, editor, similarityScore, fullCount)
				.where(
					TB_COMMUNICATION.PROJECT_ID.eq(projectId)
						.and(TB_COMMUNICATION.ALERT_ID.eq(alertId))
						.and(visibleCondition(TB_COMMUNICATION.VISIBLE, visibilitySearched))
						.and(searchCondition(textSearched))
						.and(dateRangeCondition(startDateTimeSearched, endDateTimeSearched))
				)
				.orderBy(similarityScore.desc(), TB_COMMUNICATION.DATE_TIME.desc())
				.limit(limit).offset(offset)
		).map { it.toEntity(activity, alert, creator, editor, project, fullCount) }
	}

	fun countAllByAlertId(
		projectId: UUID,
		alertId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?,
	): Mono<Long> = Mono.from(
		dsl.select(count(TB_COMMUNICATION.ID))
			.from(TB_COMMUNICATION)
			.where(
				TB_COMMUNICATION.PROJECT_ID.eq(projectId)
					.and(TB_COMMUNICATION.ALERT_ID.eq(alertId))
					.and(visibleCondition(TB_COMMUNICATION.VISIBLE, visibilitySearched))
					.and(searchCondition(textSearched))
					.and(dateRangeCondition(startDateTimeSearched, endDateTimeSearched))
			)
	).map { it.value1().toLong() }

	fun findAllByIds(projectId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<CommunicationEntity> {
		if (ids.isEmpty()) return Flux.empty()
		val activity = activityAlias()
		val alert = alertAlias()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		return Flux.from(
			baseSelect(activity, alert, project, creator, editor)
				.where(
					TB_COMMUNICATION.PROJECT_ID.eq(projectId)
						.and(TB_COMMUNICATION.ID.`in`(ids))
						.and(visibleCondition(TB_COMMUNICATION.VISIBLE, visibilitySearched))
				)
		).map { it.toEntity(activity, alert, creator, editor, project) }
	}

	fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<CommunicationEntity> {
		val activity = activityAlias()
		val alert = alertAlias()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		return Mono.from(
			baseSelect(activity, alert, project, creator, editor)
				.where(
					TB_COMMUNICATION.PROJECT_ID.eq(projectId)
						.and(TB_COMMUNICATION.ID.eq(id))
						.and(visibleCondition(TB_COMMUNICATION.VISIBLE, visibilitySearched))
				)
		).map { it.toEntity(activity, alert, creator, editor, project) }
	}

	fun findOrphan(): Flux<UUID> = Flux.from(
		dsl.select(TB_COMMUNICATION.ID).from(TB_COMMUNICATION)
			.where(TB_COMMUNICATION.MOVEMENT_ID.isNull.and(TB_COMMUNICATION.ALERT_ID.isNull))
	).map { it.value1()!! }

	fun findOrphanExcludingMovements(movementsToExclude: List<UUID>): Flux<UUID> = Flux.from(
		dsl.select(TB_COMMUNICATION.ID).from(TB_COMMUNICATION)
			.where(
				TB_COMMUNICATION.MOVEMENT_ID.isNull.or(TB_COMMUNICATION.MOVEMENT_ID.`in`(movementsToExclude))
					.and(TB_COMMUNICATION.ALERT_ID.isNull)
			)
	).map { it.value1()!! }

	fun findOrphanExcludingAlerts(alertsToExclude: List<UUID>): Flux<UUID> = Flux.from(
		dsl.select(TB_COMMUNICATION.ID).from(TB_COMMUNICATION)
			.where(
				TB_COMMUNICATION.MOVEMENT_ID.isNull
					.and(TB_COMMUNICATION.ALERT_ID.isNull.or(TB_COMMUNICATION.ALERT_ID.`in`(alertsToExclude)))
			)
	).map { it.value1()!! }

	fun findOrphanExcludingMovementsAndAlerts(movementsToExclude: List<UUID>, alertsToExclude: List<UUID>): Flux<UUID> =
		Flux.from(
			dsl.select(TB_COMMUNICATION.ID).from(TB_COMMUNICATION)
				.where(
					TB_COMMUNICATION.MOVEMENT_ID.isNull.or(TB_COMMUNICATION.MOVEMENT_ID.`in`(movementsToExclude))
						.and(TB_COMMUNICATION.ALERT_ID.isNull.or(TB_COMMUNICATION.ALERT_ID.`in`(alertsToExclude)))
				)
		).map { it.value1()!! }

	fun save(entity: CommunicationEntity): Mono<CommunicationEntity> =
		if (entity.id == null) insert(entity) else update(entity)

	private fun insert(entity: CommunicationEntity): Mono<CommunicationEntity> {
		val record = dsl.newRecord(TB_COMMUNICATION)
		record.setGeneric(entity, columns)
		record.setGenericProject(entity, TB_COMMUNICATION.PROJECT_ID)
		entity.dateTime?.let { record.set(TB_COMMUNICATION.DATE_TIME, it) }
		entity.message?.let { record.set(TB_COMMUNICATION.MESSAGE, it) }
		entity.movementId?.let { record.set(TB_COMMUNICATION.MOVEMENT_ID, it) }
		entity.alertId?.let { record.set(TB_COMMUNICATION.ALERT_ID, it) }
		return Mono.from(dsl.insertInto(TB_COMMUNICATION).set(record).returning()).map { it.toEntity() }
	}

	private fun update(entity: CommunicationEntity): Mono<CommunicationEntity> = Mono.from(
		dsl.update(TB_COMMUNICATION)
			.setGeneric(entity, columns)
			.setGenericProject(entity, TB_COMMUNICATION.PROJECT_ID)
			.set(TB_COMMUNICATION.DATE_TIME, entity.dateTime)
			.set(TB_COMMUNICATION.MESSAGE, entity.message)
			.set(TB_COMMUNICATION.MOVEMENT_ID, entity.movementId)
			.set(TB_COMMUNICATION.ALERT_ID, entity.alertId)
			.where(TB_COMMUNICATION.ID.eq(entity.id))
			.returning()
	).map { it.toEntity() }

	fun deleteById(id: UUID): Mono<Unit> =
		Mono.from(dsl.deleteFrom(TB_COMMUNICATION).where(TB_COMMUNICATION.ID.eq(id))).map { }

	private fun Record.toEntity(
		activity: TbActivity? = null,
		alert: TbAlert? = null,
		creator: TbUser? = null,
		editor: TbUser? = null,
		project: TbProject? = null,
		fullCount: Field<Int>? = null,
	): CommunicationEntity = CommunicationEntity(
		dateTime = get(TB_COMMUNICATION.DATE_TIME),
		message = get(TB_COMMUNICATION.MESSAGE),
		movementId = get(TB_COMMUNICATION.MOVEMENT_ID),
		activityId = activity?.let { get(it.ID) },
		activityName = activity?.let { get(it.NAME) },
		activityDescription = activity?.let { get(it.DESCRIPTION) },
		activityDuration = activity?.let { get(it.DURATION) },
		activityMinAllowedParticipants = activity?.let { get(it.MIN_ALLOWED_PARTICIPANTS) },
		activityMaxAllowedParticipants = activity?.let { get(it.MAX_ALLOWED_PARTICIPANTS) },
		activityStartAvailabilityDate = activity?.let { get(it.START_AVAILABILITY_DATE) },
		activityStartAvailabilityTime = activity?.let { get(it.START_AVAILABILITY_TIME) },
		activityEndAvailabilityDate = activity?.let { get(it.END_AVAILABILITY_DATE) },
		activityEndAvailabilityTime = activity?.let { get(it.END_AVAILABILITY_TIME) },
		alertId = get(TB_COMMUNICATION.ALERT_ID),
		alertDateTime = alert?.let { get(it.DATE_TIME) },
		alertTitle = alert?.let { get(it.TITLE) },
		alertStatus = alert?.let { get(it.STATUS) },
	).apply {
		fillGeneric(this, columns, creator, editor, fullCount)
		fillGenericProject(this, TB_COMMUNICATION.PROJECT_ID, project)
	}
}
