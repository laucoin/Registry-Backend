package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository

import fr.laucoin.registry.backend.domain.enumeration.AlertSortFieldEnum
import fr.laucoin.registry.backend.domain.enumeration.AlertSortFieldEnum.CREATED_DATE
import fr.laucoin.registry.backend.domain.enumeration.AlertSortFieldEnum.DATE_TIME
import fr.laucoin.registry.backend.domain.enumeration.AlertSortFieldEnum.LAST_MODIFIED_DATE
import fr.laucoin.registry.backend.domain.enumeration.AlertSortFieldEnum.TITLE
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.SortDirectionEnum.DESC
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.routines.references.similarity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbProject
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbUser
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_ALERT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_COMMUNICATION
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
import org.jooq.OrderField
import org.jooq.Record
import org.jooq.impl.DSL
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.max
import org.jooq.impl.DSL.name
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@Repository
class AlertJooqRepository(private val dsl: DSLContext) {
	private val columns = GenericColumns(TB_ALERT.ID, TB_ALERT.VISIBLE, TB_ALERT.CREATED_DATE, TB_ALERT.CREATED_BY, TB_ALERT.LAST_MODIFIED_DATE, TB_ALERT.LAST_MODIFIED_BY)

	private fun searchConditions(
		projectId: UUID,
		textSearched: String?,
		statusSearched: List<AlertStatusEnum>?,
		visibilitySearched: Boolean?,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?,
	): Condition {
		val conditions = mutableListOf(TB_ALERT.PROJECT_ID.eq(projectId))
		conditions += visibleCondition(TB_ALERT.VISIBLE, visibilitySearched)
		textSearched?.let { conditions += similarity(TB_ALERT.SEARCH_TEXT, DSL.`val`(it)).gt(0f) }
		statusSearched?.let { conditions += TB_ALERT.STATUS.`in`(it) }
		startDateTimeSearched?.let { conditions += TB_ALERT.DATE_TIME.ge(it) }
		endDateTimeSearched?.let { conditions += TB_ALERT.DATE_TIME.le(it) }
		return DSL.and(conditions)
	}

	private fun AlertSortFieldEnum.toJooqField(): Field<*> = when (this) {
		DATE_TIME -> TB_ALERT.DATE_TIME
		TITLE -> TB_ALERT.TITLE
		CREATED_DATE -> TB_ALERT.CREATED_DATE
		LAST_MODIFIED_DATE -> TB_ALERT.LAST_MODIFIED_DATE
	}

	// similarityScore sorts first (a no-op constant when there's no text search), TB_ALERT.DATE_TIME
	// (descending) always stays last as the final tiebreaker (v1's sole, implicit order).
	private fun orderFields(
		similarityScore: Field<Float>,
		sortFields: List<SortModel<AlertSortFieldEnum>>,
	): List<OrderField<*>> {
		val fields = mutableListOf<OrderField<*>>(similarityScore.desc())
		sortFields.forEach { fields += if (it.direction == DESC) it.field.toJooqField().desc() else it.field.toJooqField().asc() }
		fields += TB_ALERT.DATE_TIME.desc()
		return fields
	}

	fun findAll(
		projectId: UUID,
		textSearched: String?,
		statusSearched: List<AlertStatusEnum>?,
		visibilitySearched: Boolean?,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?,
		sortFields: List<SortModel<AlertSortFieldEnum>> = emptyList(),
		limit: Int,
		offset: Int,
	): Flux<AlertEntity> {
		val creator = creatorTable()
		val editor = editorTable()
		val project = projectTable()
		val similarityScore = (if (textSearched == null) DSL.inline(1f) else similarity(
			TB_ALERT.SEARCH_TEXT,
			DSL.`val`(textSearched)
		)).`as`("similarity_score")
		val fullCount = count().over().`as`("full_count")
		return Flux.from(
			dsl.select(
				TB_ALERT.asterisk(),
				similarityScore,
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
				fullCount,
			)
				.from(TB_ALERT)
				.join(project).on(TB_ALERT.PROJECT_ID.eq(project.ID).and(project.VISIBLE.isTrue))
				.leftJoin(creator).on(TB_ALERT.CREATED_BY.eq(creator.ID))
				.leftJoin(editor).on(TB_ALERT.LAST_MODIFIED_BY.eq(editor.ID))
				.where(
					searchConditions(
						projectId,
						textSearched,
						statusSearched,
						visibilitySearched,
						startDateTimeSearched,
						endDateTimeSearched
					)
				)
				.orderBy(orderFields(similarityScore, sortFields))
				.limit(limit).offset(offset)
		).map { it.toEntity(creator, editor, project, fullCount) }
	}

	fun findWithLimit(
		projectId: UUID,
		textSearched: String?,
		statusSearched: List<AlertStatusEnum>?,
		visibilitySearched: Boolean?,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?,
		limit: Int,
	): Flux<AlertEntity> {
		val creator = creatorTable()
		val editor = editorTable()
		val project = projectTable()
		val similarityScore = (if (textSearched == null) DSL.inline(1f) else similarity(
			TB_ALERT.SEARCH_TEXT,
			DSL.`val`(textSearched)
		)).`as`("similarity_score")
		return Flux.from(
			dsl.select(
				TB_ALERT.asterisk(),
				similarityScore,
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
				.from(TB_ALERT)
				.join(project).on(TB_ALERT.PROJECT_ID.eq(project.ID).and(project.VISIBLE.isTrue))
				.leftJoin(creator).on(TB_ALERT.CREATED_BY.eq(creator.ID))
				.leftJoin(editor).on(TB_ALERT.LAST_MODIFIED_BY.eq(editor.ID))
				.where(
					searchConditions(
						projectId,
						textSearched,
						statusSearched,
						visibilitySearched,
						startDateTimeSearched,
						endDateTimeSearched
					)
				)
				.orderBy(similarityScore.desc(), TB_ALERT.DATE_TIME.desc())
				.limit(limit)
		).map { it.toEntity(creator, editor, project) }
	}

	fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<AlertEntity> {
		val creator = creatorTable()
		val editor = editorTable()
		val project = projectTable()
		return Mono.from(
			dsl.select(
				TB_ALERT.asterisk(),
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
				.from(TB_ALERT)
				.join(project).on(TB_ALERT.PROJECT_ID.eq(project.ID).and(project.VISIBLE.isTrue))
				.leftJoin(creator).on(TB_ALERT.CREATED_BY.eq(creator.ID))
				.leftJoin(editor).on(TB_ALERT.LAST_MODIFIED_BY.eq(editor.ID))
				.where(
					TB_ALERT.PROJECT_ID.eq(projectId).and(visibleCondition(TB_ALERT.VISIBLE, visibilitySearched))
						.and(TB_ALERT.ID.eq(id))
				)
		).map { it.toEntity(creator, editor, project) }
	}

	fun findOlderThanAndUncommentedSince(dateThreshold: LocalDate): Flux<UUID> {
		val lastComment = max(TB_COMMUNICATION.DATE_TIME).`as`("max")
		val lc = dsl.select(lastComment, TB_COMMUNICATION.ALERT_ID)
			.from(TB_COMMUNICATION)
			.where(TB_COMMUNICATION.ALERT_ID.isNotNull)
			.groupBy(TB_COMMUNICATION.ALERT_ID)
			.asTable("lc")
		val lcMax = field(name("lc", "max"), ZonedDateTime::class.java)
		val lcAlertId = field(name("lc", "alert_id"), UUID::class.java)
		val thresholdAsTimestamp = DSL.`val`(dateThreshold).cast(TB_ALERT.LAST_MODIFIED_DATE)
		return Flux.from(
			dsl.select(TB_ALERT.ID)
				.from(TB_ALERT)
				.leftJoin(lc).on(lcAlertId.eq(TB_ALERT.ID))
				.where(
					lcMax.isNull.or(lcMax.lt(thresholdAsTimestamp))
						.and(TB_ALERT.LAST_MODIFIED_DATE.lt(thresholdAsTimestamp))
				)
		).map { it.value1()!! }
	}

	fun save(entity: AlertEntity): Mono<AlertEntity> = if (entity.id == null) insert(entity) else update(entity)

	private fun insert(entity: AlertEntity): Mono<AlertEntity> {
		val record = dsl.newRecord(TB_ALERT)
		record.setGeneric(entity, columns)
		record.setGenericProject(entity, TB_ALERT.PROJECT_ID)
		entity.dateTime?.let { record.set(TB_ALERT.DATE_TIME, it) }
		entity.title?.let { record.set(TB_ALERT.TITLE, it) }
		entity.status?.let { record.set(TB_ALERT.STATUS, it) }
		return Mono.from(dsl.insertInto(TB_ALERT).set(record).returning()).map { it.toEntity() }
	}

	private fun update(entity: AlertEntity): Mono<AlertEntity> = Mono.from(
		dsl.update(TB_ALERT)
			.setGeneric(entity, columns)
			.setGenericProject(entity, TB_ALERT.PROJECT_ID)
			.set(TB_ALERT.DATE_TIME, entity.dateTime)
			.set(TB_ALERT.TITLE, entity.title)
			.set(TB_ALERT.STATUS, entity.status)
			.where(TB_ALERT.ID.eq(entity.id))
			.returning()
	).map { it.toEntity() }

	fun deleteById(id: UUID): Mono<Unit> = Mono.from(dsl.deleteFrom(TB_ALERT).where(TB_ALERT.ID.eq(id))).map { }

	private fun Record.toEntity(
		creator: TbUser? = null,
		editor: TbUser? = null,
		project: TbProject? = null,
		fullCount: Field<Int>? = null,
	): AlertEntity = AlertEntity(
		dateTime = get(TB_ALERT.DATE_TIME),
		title = get(TB_ALERT.TITLE),
		status = get(TB_ALERT.STATUS),
	).apply {
		fillGeneric(this, columns, creator, editor, fullCount)
		fillGenericProject(this, TB_ALERT.PROJECT_ID, project)
	}
}
