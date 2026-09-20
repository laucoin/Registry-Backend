package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericProjectEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbProject
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbUser
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_PROJECT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_USER
import org.jooq.Condition
import org.jooq.Field
import org.jooq.Record
import org.jooq.TableField
import org.jooq.UpdateSetMoreStep
import org.jooq.UpdateSetStep
import org.jooq.impl.DSL
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.util.UUID

object GenericJooqQueries {
	fun creatorTable(): TbUser = TB_USER.`as`("create_tb")

	fun editorTable(): TbUser = TB_USER.`as`("editor_tb")

	fun projectTable(): TbProject = TB_PROJECT.`as`("project_tb")

	fun visibleCondition(field: TableField<*, Boolean?>, visibilitySearched: Boolean?): Condition =
		visibilitySearched?.let { field.eq(it) } ?: DSL.noCondition()

	fun startedCondition(startDate: Field<LocalDate?>, startTime: Field<OffsetTime?>): Condition =
		startDate.isNull.or(startDate.lt(DSL.currentLocalDate()))
			.or(startDate.eq(DSL.currentLocalDate()).and(startTime.isNull.or(startTime.le(DSL.currentOffsetTime()))))

	private fun notEndedCondition(endDate: Field<LocalDate?>, endTime: Field<OffsetTime?>): Condition =
		endDate.isNull.or(endDate.gt(DSL.currentLocalDate()))
			.or(endDate.eq(DSL.currentLocalDate()).and(endTime.isNull.or(endTime.ge(DSL.currentOffsetTime()))))

	fun activeNowCondition(
		startDate: Field<LocalDate?>,
		startTime: Field<OffsetTime?>,
		endDate: Field<LocalDate?>,
		endTime: Field<OffsetTime?>
	): Condition =
		startedCondition(startDate, startTime).and(notEndedCondition(endDate, endTime))

	fun activeAtCondition(
		startDate: Field<LocalDate?>,
		startTime: Field<OffsetTime?>,
		endDate: Field<LocalDate?>,
		endTime: Field<OffsetTime?>,
		at: ZonedDateTime
	): Condition {
		val date = at.toLocalDate()
		val time = at.toOffsetDateTime().toOffsetTime()
		val startsBefore =
			startDate.isNull.or(startDate.lt(date)).or(startDate.eq(date).and(startTime.isNull.or(startTime.le(time))))
		val endsAfter =
			endDate.isNull.or(endDate.gt(date)).or(endDate.eq(date).and(endTime.isNull.or(endTime.ge(time))))
		return startsBefore.and(endsAfter)
	}

	class GenericColumns(
		val id: Field<UUID?>,
		val visible: Field<Boolean?>,
		val createdDate: Field<ZonedDateTime?>,
		val createdBy: Field<UUID?>,
		val lastModifiedDate: Field<ZonedDateTime?>,
		val lastModifiedBy: Field<UUID?>,
	)

	fun Record.fillGeneric(
		entity: GenericEntity,
		columns: GenericColumns,
		creator: TbUser? = null,
		editor: TbUser? = null,
		fullCount: Field<Int>? = null,
	) {
		entity.id = get(columns.id)
		entity.visible = get(columns.visible)
		entity.createdAt = get(columns.createdDate)
		entity.creatorId = get(columns.createdBy)
		entity.creatorFirstName = creator?.let { get(it.FIRST_NAME) }
		entity.creatorLastName = creator?.let { get(it.LAST_NAME) }
		entity.creatorEmail = creator?.let { get(it.EMAIL) }
		entity.lastUpdateAt = get(columns.lastModifiedDate)
		entity.lastEditorId = get(columns.lastModifiedBy)
		entity.lastEditorFirstName = editor?.let { get(it.FIRST_NAME) }
		entity.lastEditorLastName = editor?.let { get(it.LAST_NAME) }
		entity.lastEditorEmail = editor?.let { get(it.EMAIL) }
		entity.fullCount = fullCount?.let { get(it)?.toLong() }
	}

	fun Record.fillGenericProject(entity: GenericProjectEntity, projectId: Field<UUID?>, project: TbProject? = null) {
		entity.projectId = get(projectId)
		entity.projectName = project?.let { get(it.NAME) }
		entity.projectStartDate = project?.let { get(it.BEGIN_DATE) }
		entity.projectStartTime = project?.let { get(it.BEGIN_TIME) }
		entity.projectEndDate = project?.let { get(it.END_DATE) }
		entity.projectEndTime = project?.let { get(it.END_TIME) }
		entity.projectOptions = project?.let { get(it.OPTIONS) }
	}

	fun Record.setGeneric(entity: GenericEntity, columns: GenericColumns) {
		entity.visible?.let { set(columns.visible, it) }
		entity.createdAt?.let { set(columns.createdDate, it) }
		entity.creatorId?.let { set(columns.createdBy, it) }
		entity.lastUpdateAt?.let { set(columns.lastModifiedDate, it) }
		entity.lastEditorId?.let { set(columns.lastModifiedBy, it) }
	}

	fun Record.setGenericProject(entity: GenericProjectEntity, projectId: Field<UUID?>) {
		entity.projectId?.let { set(projectId, it) }
	}

	fun <R : Record> UpdateSetStep<R>.setGeneric(entity: GenericEntity, columns: GenericColumns): UpdateSetMoreStep<R> =
		set(columns.visible, entity.visible)
			.set(columns.createdDate, entity.createdAt)
			.set(columns.createdBy, entity.creatorId)
			.set(columns.lastModifiedDate, entity.lastUpdateAt)
			.set(columns.lastModifiedBy, entity.lastEditorId)

	fun <R : Record> UpdateSetStep<R>.setGenericProject(
		entity: GenericProjectEntity,
		projectId: Field<UUID?>
	): UpdateSetMoreStep<R> =
		set(projectId, entity.projectId)
}
