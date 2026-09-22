package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository

import fr.laucoin.registry.backend.domain.enumeration.ProjectSortFieldEnum
import fr.laucoin.registry.backend.domain.enumeration.ProjectSortFieldEnum.CREATED_DATE
import fr.laucoin.registry.backend.domain.enumeration.ProjectSortFieldEnum.LAST_MODIFIED_DATE
import fr.laucoin.registry.backend.domain.enumeration.ProjectSortFieldEnum.NAME
import fr.laucoin.registry.backend.domain.enumeration.SortDirectionEnum.DESC
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectRelationEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.routines.references.unaccent2
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbUser
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_ACTIVITY
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_ALERT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_COMMUNICATION
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_GROUP
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_PARTICIPANT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_PROJECT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_PROJECT_PROFILE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_VEHICLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.GenericColumns
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.activeAtCondition
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.creatorTable
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.editorTable
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.fillGeneric
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.setGeneric
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
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.util.UUID

@Repository
class ProjectJooqRepository(private val dsl: DSLContext) {
	private val columns = GenericColumns(
		TB_PROJECT.ID,
		TB_PROJECT.VISIBLE,
		TB_PROJECT.CREATED_DATE,
		TB_PROJECT.CREATED_BY,
		TB_PROJECT.LAST_MODIFIED_DATE,
		TB_PROJECT.LAST_MODIFIED_BY
	)

	private fun searchConditions(
		textSearched: String?,
		visibilitySearched: Boolean?,
		dateTimeSearched: ZonedDateTime?
	): Condition {
		val conditions = mutableListOf<Condition>()
		textSearched?.let {
			val pattern = DSL.concat(DSL.inline("%"), unaccent2(DSL.`val`(it)), DSL.inline("%"))
			conditions += unaccent2(TB_PROJECT.NAME).likeIgnoreCase(pattern)
		}
		conditions += visibleCondition(TB_PROJECT.VISIBLE, visibilitySearched)
		dateTimeSearched?.let {
			conditions += activeAtCondition(
				TB_PROJECT.BEGIN_DATE,
				TB_PROJECT.BEGIN_TIME,
				TB_PROJECT.END_DATE,
				TB_PROJECT.END_TIME,
				it
			)
		}
		return DSL.and(conditions)
	}

	private fun dateInRangeCondition(dateTimeSearched: ZonedDateTime): Condition {
		val date = dateTimeSearched.toLocalDate()
		val time = dateTimeSearched.toOffsetDateTime().toOffsetTime()
		val startsBefore = TB_PROJECT.BEGIN_DATE.isNull
			.or(TB_PROJECT.BEGIN_DATE.lt(date))
			.or(TB_PROJECT.BEGIN_DATE.eq(date).and(TB_PROJECT.BEGIN_TIME.isNull.or(TB_PROJECT.BEGIN_TIME.le(time))))
		val endsAfter = TB_PROJECT.END_DATE.isNull
			.or(TB_PROJECT.END_DATE.gt(date))
			.or(TB_PROJECT.END_DATE.eq(date).and(TB_PROJECT.END_TIME.isNull.or(TB_PROJECT.END_TIME.ge(time))))
		return startsBefore.and(endsAfter)
	}

	private fun ProjectSortFieldEnum.toJooqField(): Field<*> = when (this) {
		NAME -> TB_PROJECT.NAME
		CREATED_DATE -> TB_PROJECT.CREATED_DATE
		LAST_MODIFIED_DATE -> TB_PROJECT.LAST_MODIFIED_DATE
	}

	// TB_PROJECT.NAME always stays as the final tiebreaker (v1's sole, implicit order).
	private fun orderFields(sortFields: List<SortModel<ProjectSortFieldEnum>>): List<OrderField<*>> {
		val fields = mutableListOf<OrderField<*>>()
		sortFields.forEach { fields += if (it.direction == DESC) it.field.toJooqField().desc() else it.field.toJooqField().asc() }
		fields += TB_PROJECT.NAME
		return fields
	}

	fun findAll(
		textSearched: String?,
		visibilitySearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
		sortFields: List<SortModel<ProjectSortFieldEnum>> = emptyList(),
		limit: Int,
		offset: Int,
	): Flux<ProjectEntity> {
		val creator = creatorTable()
		val editor = editorTable()
		val fullCount = count().over().`as`("full_count")
		return Flux.from(
			dsl.select(
				TB_PROJECT.asterisk(),
				creator.FIRST_NAME,
				creator.LAST_NAME,
				creator.EMAIL,
				editor.FIRST_NAME,
				editor.LAST_NAME,
				editor.EMAIL,
				fullCount
			)
				.from(TB_PROJECT)
				.leftJoin(creator).on(TB_PROJECT.CREATED_BY.eq(creator.ID))
				.leftJoin(editor).on(TB_PROJECT.LAST_MODIFIED_BY.eq(editor.ID))
				.where(searchConditions(textSearched, visibilitySearched, dateTimeSearched))
				.orderBy(orderFields(sortFields))
				.limit(limit).offset(offset)
		).map { it.toEntity(creator, editor, fullCount) }
	}

	fun findAllInProjectIds(
		projectIds: List<UUID>,
		textSearched: String?,
		visibilitySearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
		sortFields: List<SortModel<ProjectSortFieldEnum>> = emptyList(),
		limit: Int,
		offset: Int,
	): Flux<ProjectEntity> {
		val creator = creatorTable()
		val editor = editorTable()
		val fullCount = count().over().`as`("full_count")
		return Flux.from(
			dsl.select(
				TB_PROJECT.asterisk(),
				creator.FIRST_NAME,
				creator.LAST_NAME,
				creator.EMAIL,
				editor.FIRST_NAME,
				editor.LAST_NAME,
				editor.EMAIL,
				fullCount
			)
				.from(TB_PROJECT)
				.leftJoin(creator).on(TB_PROJECT.CREATED_BY.eq(creator.ID))
				.leftJoin(editor).on(TB_PROJECT.LAST_MODIFIED_BY.eq(editor.ID))
				.where(TB_PROJECT.ID.`in`(projectIds).and(searchConditions(textSearched, visibilitySearched, dateTimeSearched)))
				.orderBy(orderFields(sortFields))
				.limit(limit).offset(offset)
		).map { it.toEntity(creator, editor, fullCount) }
	}

	fun findById(id: UUID, visibilitySearched: Boolean?): Mono<ProjectEntity> {
		val creator = creatorTable()
		val editor = editorTable()
		return Mono.from(
			dsl.select(
				TB_PROJECT.asterisk(),
				creator.FIRST_NAME,
				creator.LAST_NAME,
				creator.EMAIL,
				editor.FIRST_NAME,
				editor.LAST_NAME,
				editor.EMAIL
			)
				.from(TB_PROJECT)
				.leftJoin(creator).on(TB_PROJECT.CREATED_BY.eq(creator.ID))
				.leftJoin(editor).on(TB_PROJECT.LAST_MODIFIED_BY.eq(editor.ID))
				.where(TB_PROJECT.ID.eq(id).and(visibleCondition(TB_PROJECT.VISIBLE, visibilitySearched)))
		).map { it.toEntity(creator, editor) }
	}

	private fun isOutOfPostgresRange(value: ZonedDateTime): Boolean = value.year !in -4700..294000

	private fun dateField(value: ZonedDateTime?): Field<LocalDate?> = when {
		value == null -> DSL.`val`(null as LocalDate?)
		!isOutOfPostgresRange(value) -> DSL.`val`(value.toLocalDate())
		value.year > 0 -> DSL.field("'infinity'::date", LocalDate::class.java)
		else -> DSL.field("'-infinity'::date", LocalDate::class.java)
	}

	private fun timeField(value: ZonedDateTime?): Field<OffsetTime?> = when {
		value == null -> DSL.`val`(null as OffsetTime?)
		!isOutOfPostgresRange(value) -> DSL.`val`(value.toOffsetDateTime().toOffsetTime())
		else -> DSL.`val`(OffsetTime.of(0, 0, 0, 0, java.time.ZoneOffset.UTC))
	}

	private fun availabilityNonOverlapCondition(
		startDate: Field<LocalDate?>,
		startTime: Field<OffsetTime?>,
		endDate: Field<LocalDate?>,
		endTime: Field<OffsetTime?>,
		begin: ZonedDateTime?,
		end: ZonedDateTime?,
	): Condition {
		val endDateVal = dateField(end)
		val endTimeVal = timeField(end)
		val beginDateVal = dateField(begin)
		val beginTimeVal = timeField(begin)
		return DSL.condition(
			"COALESCE({0}, '-infinity'::date) > {1} OR (COALESCE({0}, '-infinity'::date) = {1} AND COALESCE({2}, '00:00:00.000000'::time) > {3}) " +
					"OR COALESCE({4}, '+infinity'::date) < {5} OR (COALESCE({4}, '+infinity'::date) = {5} AND COALESCE({6}, '23:59:59.999999'::time) < {7})",
			startDate, endDateVal, startTime, endTimeVal, endDate, beginDateVal, endTime, beginTimeVal,
		)
	}

	private fun dateTimeNonOverlapCondition(
		dateTime: Field<ZonedDateTime?>,
		begin: ZonedDateTime?,
		end: ZonedDateTime?
	): Condition {
		val beginDateTime = if (begin != null && isOutOfPostgresRange(begin)) DSL.field(
			if (begin.year > 0) "'infinity'::timestamptz" else "'-infinity'::timestamptz",
			ZonedDateTime::class.java
		)
		else DSL.`val`(begin)
		val endDateTime = if (end != null && isOutOfPostgresRange(end)) DSL.field(
			if (end.year > 0) "'infinity'::timestamptz" else "'-infinity'::timestamptz",
			ZonedDateTime::class.java
		)
		else DSL.`val`(end)
		return dateTime.lt(beginDateTime).or(dateTime.gt(endDateTime))
	}

	fun validDateTime(id: UUID, begin: ZonedDateTime?, end: ZonedDateTime?): Mono<ProjectRelationEntity> {
		val profileIds = dsl.select(TB_PROJECT_PROFILE.ID).from(TB_PROJECT_PROFILE)
			.where(
				TB_PROJECT_PROFILE.PROJECT_ID.eq(id).and(
					availabilityNonOverlapCondition(
						TB_PROJECT_PROFILE.START_ACCESS_DATE,
						TB_PROJECT_PROFILE.START_ACCESS_TIME,
						TB_PROJECT_PROFILE.END_ACCESS_DATE,
						TB_PROJECT_PROFILE.END_ACCESS_TIME,
						begin,
						end
					)
				)
			)
		val movementIds = dsl.select(TB_MOVEMENT.ID).from(TB_MOVEMENT)
			.where(TB_MOVEMENT.PROJECT_ID.eq(id).and(dateTimeNonOverlapCondition(TB_MOVEMENT.DATE_TIME, begin, end)))
		val participantIds = dsl.select(TB_PARTICIPANT.ID).from(TB_PARTICIPANT)
			.where(
				TB_PARTICIPANT.PROJECT_ID.eq(id).and(
					availabilityNonOverlapCondition(
						TB_PARTICIPANT.START_AVAILABILITY_DATE,
						TB_PARTICIPANT.START_AVAILABILITY_TIME,
						TB_PARTICIPANT.END_AVAILABILITY_DATE,
						TB_PARTICIPANT.END_AVAILABILITY_TIME,
						begin,
						end
					)
				)
			)
		val groupIds = dsl.select(TB_GROUP.ID).from(TB_GROUP)
			.where(
				TB_GROUP.PROJECT_ID.eq(id).and(
					availabilityNonOverlapCondition(
						TB_GROUP.START_AVAILABILITY_DATE,
						TB_GROUP.START_AVAILABILITY_TIME,
						TB_GROUP.END_AVAILABILITY_DATE,
						TB_GROUP.END_AVAILABILITY_TIME,
						begin,
						end
					)
				)
			)
		val vehicleIds = dsl.select(TB_VEHICLE.ID).from(TB_VEHICLE)
			.where(
				TB_VEHICLE.PROJECT_ID.eq(id).and(
					availabilityNonOverlapCondition(
						TB_VEHICLE.START_AVAILABILITY_DATE,
						TB_VEHICLE.START_AVAILABILITY_TIME,
						TB_VEHICLE.END_AVAILABILITY_DATE,
						TB_VEHICLE.END_AVAILABILITY_TIME,
						begin,
						end
					)
				)
			)
		val activityIds = dsl.select(TB_ACTIVITY.ID).from(TB_ACTIVITY)
			.where(
				TB_ACTIVITY.PROJECT_ID.eq(id).and(
					availabilityNonOverlapCondition(
						TB_ACTIVITY.START_AVAILABILITY_DATE,
						TB_ACTIVITY.START_AVAILABILITY_TIME,
						TB_ACTIVITY.END_AVAILABILITY_DATE,
						TB_ACTIVITY.END_AVAILABILITY_TIME,
						begin,
						end
					)
				)
			)
		val communicationIds = dsl.select(TB_COMMUNICATION.ID).from(TB_COMMUNICATION)
			.where(
				TB_COMMUNICATION.PROJECT_ID.eq(id)
					.and(dateTimeNonOverlapCondition(TB_COMMUNICATION.DATE_TIME, begin, end))
			)
		val alertIds = dsl.select(TB_ALERT.ID).from(TB_ALERT)
			.where(TB_ALERT.PROJECT_ID.eq(id).and(dateTimeNonOverlapCondition(TB_ALERT.DATE_TIME, begin, end)))

		val union =
			profileIds.union(movementIds).union(participantIds).union(groupIds).union(vehicleIds).union(activityIds)
				.union(communicationIds).union(alertIds)
				.asTable("t")
		val idField = field(name("t", "id"), UUID::class.java)
		return Mono.from(dsl.select(count(idField)).from(union)).map { ProjectRelationEntity(count = it.value1()) }
	}

	fun findProjectsEligibleForPurge(dateThreshold: LocalDate): Flux<UUID> {
		fun lastChangeCte(
			cteName: String,
			projectIdField: org.jooq.TableField<*, UUID?>,
			lastModifiedField: Field<ZonedDateTime?>
		) =
			name(cteName).`as`(
				dsl.select(projectIdField, max(lastModifiedField).`as`("date_time"))
					.from(projectIdField.table)
					.groupBy(projectIdField)
			)

		val lastProfileChange =
			lastChangeCte("last_profile_change", TB_PROJECT_PROFILE.PROJECT_ID, TB_PROJECT_PROFILE.LAST_MODIFIED_DATE)
		val lastMovementChange =
			lastChangeCte("last_movement_change", TB_MOVEMENT.PROJECT_ID, TB_MOVEMENT.LAST_MODIFIED_DATE)
		val lastParticipantChange =
			lastChangeCte("last_participant_change", TB_PARTICIPANT.PROJECT_ID, TB_PARTICIPANT.LAST_MODIFIED_DATE)
		val lastGroupChange = lastChangeCte("last_group_change", TB_GROUP.PROJECT_ID, TB_GROUP.LAST_MODIFIED_DATE)
		val lastVehicleChange =
			lastChangeCte("last_vehicle_change", TB_VEHICLE.PROJECT_ID, TB_VEHICLE.LAST_MODIFIED_DATE)
		val lastActivityChange =
			lastChangeCte("last_activity_change", TB_ACTIVITY.PROJECT_ID, TB_ACTIVITY.LAST_MODIFIED_DATE)
		val lastCommunicationChange =
			lastChangeCte("last_communication_change", TB_COMMUNICATION.PROJECT_ID, TB_COMMUNICATION.LAST_MODIFIED_DATE)
		val lastAlertChange = lastChangeCte("last_alert_change", TB_ALERT.PROJECT_ID, TB_ALERT.LAST_MODIFIED_DATE)

		fun dateTimeOf(cteName: String) = field(name(cteName, "date_time"), ZonedDateTime::class.java)
		fun projectIdOf(cteName: String) = field(name(cteName, "project_id"), UUID::class.java)

		val thresholdAsTimestamp = DSL.`val`(dateThreshold).cast(TB_PROJECT.LAST_MODIFIED_DATE)

		fun outdatedCondition(cteName: String): Condition {
			val dateTime = dateTimeOf(cteName)
			return dateTime.isNull.or(dateTime.lt(thresholdAsTimestamp))
		}

		return Flux.from(
			dsl.with(
				lastProfileChange,
				lastMovementChange,
				lastParticipantChange,
				lastGroupChange,
				lastVehicleChange,
				lastActivityChange,
				lastCommunicationChange,
				lastAlertChange
			)
				.select(TB_PROJECT.ID)
				.from(TB_PROJECT)
				.leftJoin(lastProfileChange).on(TB_PROJECT.ID.eq(projectIdOf("last_profile_change")))
				.leftJoin(lastMovementChange).on(TB_PROJECT.ID.eq(projectIdOf("last_movement_change")))
				.leftJoin(lastParticipantChange).on(TB_PROJECT.ID.eq(projectIdOf("last_participant_change")))
				.leftJoin(lastGroupChange).on(TB_PROJECT.ID.eq(projectIdOf("last_group_change")))
				.leftJoin(lastVehicleChange).on(TB_PROJECT.ID.eq(projectIdOf("last_vehicle_change")))
				.leftJoin(lastActivityChange).on(TB_PROJECT.ID.eq(projectIdOf("last_activity_change")))
				.leftJoin(lastCommunicationChange).on(TB_PROJECT.ID.eq(projectIdOf("last_communication_change")))
				.leftJoin(lastAlertChange).on(TB_PROJECT.ID.eq(projectIdOf("last_alert_change")))
				.where(
					outdatedCondition("last_profile_change")
						.and(outdatedCondition("last_movement_change"))
						.and(outdatedCondition("last_participant_change"))
						.and(outdatedCondition("last_group_change"))
						.and(outdatedCondition("last_vehicle_change"))
						.and(outdatedCondition("last_activity_change"))
						.and(outdatedCondition("last_communication_change"))
						.and(outdatedCondition("last_alert_change"))
				)
		).map { it.value1()!! }
	}

	fun save(entity: ProjectEntity): Mono<ProjectEntity> = if (entity.id == null) insert(entity) else update(entity)

	private fun insert(entity: ProjectEntity): Mono<ProjectEntity> {
		val record = dsl.newRecord(TB_PROJECT)
		record.setGeneric(entity, columns)
		entity.name?.let { record.set(TB_PROJECT.NAME, it) }
		entity.beginDate?.let { record.set(TB_PROJECT.BEGIN_DATE, it) }
		entity.beginTime?.let { record.set(TB_PROJECT.BEGIN_TIME, it) }
		entity.endDate?.let { record.set(TB_PROJECT.END_DATE, it) }
		entity.endTime?.let { record.set(TB_PROJECT.END_TIME, it) }
		entity.options?.let { record.set(TB_PROJECT.OPTIONS, it) }
		return Mono.from(dsl.insertInto(TB_PROJECT).set(record).returning()).map { it.toEntity() }
	}

	private fun update(entity: ProjectEntity): Mono<ProjectEntity> = Mono.from(
		dsl.update(TB_PROJECT)
			.setGeneric(entity, columns)
			.set(TB_PROJECT.NAME, entity.name)
			.set(TB_PROJECT.BEGIN_DATE, entity.beginDate)
			.set(TB_PROJECT.BEGIN_TIME, entity.beginTime)
			.set(TB_PROJECT.END_DATE, entity.endDate)
			.set(TB_PROJECT.END_TIME, entity.endTime)
			.set(TB_PROJECT.OPTIONS, entity.options)
			.where(TB_PROJECT.ID.eq(entity.id))
			.returning()
	).map { it.toEntity() }

	fun deleteById(id: UUID): Mono<Unit> = Mono.from(dsl.deleteFrom(TB_PROJECT).where(TB_PROJECT.ID.eq(id))).map { }

	private fun Record.toEntity(
		creator: TbUser? = null,
		editor: TbUser? = null,
		fullCount: Field<Int>? = null
	): ProjectEntity = ProjectEntity(
		name = get(TB_PROJECT.NAME),
		beginDate = get(TB_PROJECT.BEGIN_DATE),
		beginTime = get(TB_PROJECT.BEGIN_TIME),
		endDate = get(TB_PROJECT.END_DATE),
		endTime = get(TB_PROJECT.END_TIME),
		options = get(TB_PROJECT.OPTIONS),
	).apply {
		fillGeneric(this, columns, creator, editor, fullCount)
	}
}
