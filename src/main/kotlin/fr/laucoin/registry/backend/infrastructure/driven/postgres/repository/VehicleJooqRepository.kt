package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.SortDirectionEnum.DESC
import fr.laucoin.registry.backend.domain.enumeration.VehicleSortFieldEnum
import fr.laucoin.registry.backend.domain.enumeration.VehicleSortFieldEnum.BRAND
import fr.laucoin.registry.backend.domain.enumeration.VehicleSortFieldEnum.CREATED_DATE
import fr.laucoin.registry.backend.domain.enumeration.VehicleSortFieldEnum.LAST_MODIFIED_DATE
import fr.laucoin.registry.backend.domain.enumeration.VehicleSortFieldEnum.LICENSE_PLATE
import fr.laucoin.registry.backend.domain.enumeration.VehicleSortFieldEnum.MODEL
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.vehicle.VehicleEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.routines.references.similarity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbProject
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbUser
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_MOVEMENT_CONTENT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_VEHICLE
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
class VehicleJooqRepository(private val dsl: DSLContext) {
	private val columns = GenericColumns(
		TB_VEHICLE.ID,
		TB_VEHICLE.VISIBLE,
		TB_VEHICLE.CREATED_DATE,
		TB_VEHICLE.CREATED_BY,
		TB_VEHICLE.LAST_MODIFIED_DATE,
		TB_VEHICLE.LAST_MODIFIED_BY
	)

	private fun lastMovementCte(): org.jooq.CommonTableExpression<*> {
		val plmDateTime = field(name("plm", "last_movement_date_time"), ZonedDateTime::class.java)
		val plmVehicleId = field(name("plm", "vehicle_id"), UUID::class.java)
		return name("last_movement").`as`(
			dsl.select(TB_MOVEMENT.TYPE, TB_MOVEMENT.DATE_TIME, plmVehicleId)
				.from(TB_MOVEMENT)
				.leftJoin(
					dsl.select(
						max(TB_MOVEMENT.DATE_TIME).`as`("last_movement_date_time"),
						TB_MOVEMENT.PROJECT_ID,
						TB_MOVEMENT_CONTENT.VEHICLE_ID,
					)
						.from(TB_MOVEMENT)
						.join(TB_MOVEMENT_CONTENT).on(TB_MOVEMENT_CONTENT.MOVEMENT_ID.eq(TB_MOVEMENT.ID))
						.where(TB_MOVEMENT.VISIBLE.isTrue)
						.groupBy(TB_MOVEMENT_CONTENT.VEHICLE_ID, TB_MOVEMENT.PROJECT_ID)
						.asTable("plm")
				).on(plmDateTime.eq(TB_MOVEMENT.DATE_TIME))
				.where(plmVehicleId.isNotNull)
		)
	}

	private fun lastMovementTypeField(): Field<String> = field(name("last_movement", "type"), String::class.java)
	private fun lastMovementDateTimeField(): Field<ZonedDateTime> =
		field(name("last_movement", "date_time"), ZonedDateTime::class.java)

	private fun lastMovementVehicleIdField(): Field<UUID> = field(name("last_movement", "vehicle_id"), UUID::class.java)

	private fun searchConditions(
		projectId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		presenceSearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
		lastMovementType: Field<String>,
	): Condition {
		val conditions = mutableListOf(TB_VEHICLE.PROJECT_ID.eq(projectId))
		textSearched?.let { conditions += similarity(TB_VEHICLE.SEARCH_TEXT, DSL.`val`(it)).gt(0f) }
		conditions += visibleCondition(TB_VEHICLE.VISIBLE, visibilitySearched)
		availabilitySearched?.let { conditions += availabilityCondition(it) }
		presenceSearched?.let { presence ->
			val isOutOrNull = lastMovementType.isNull.or(lastMovementType.eq("OUT"))
			conditions += if (presence) isOutOrNull.not() else isOutOrNull
		}
		dateTimeSearched?.let { conditions += dateInRangeCondition(it) }
		return DSL.and(conditions)
	}

	private fun availabilityCondition(availabilitySearched: Boolean): Condition {
		val isAvailable = activeNowCondition(
			TB_VEHICLE.START_AVAILABILITY_DATE,
			TB_VEHICLE.START_AVAILABILITY_TIME,
			TB_VEHICLE.END_AVAILABILITY_DATE,
			TB_VEHICLE.END_AVAILABILITY_TIME
		)
		return if (availabilitySearched) isAvailable else isAvailable.not()
	}

	private fun dateInRangeCondition(dateTimeSearched: ZonedDateTime): Condition =
		activeAtCondition(
			TB_VEHICLE.START_AVAILABILITY_DATE,
			TB_VEHICLE.START_AVAILABILITY_TIME,
			TB_VEHICLE.END_AVAILABILITY_DATE,
			TB_VEHICLE.END_AVAILABILITY_TIME,
			dateTimeSearched
		)

	private fun VehicleSortFieldEnum.toJooqField(): Field<*> = when (this) {
		BRAND -> TB_VEHICLE.BRAND
		MODEL -> TB_VEHICLE.MODEL
		LICENSE_PLATE -> TB_VEHICLE.LICENSE_PLATE
		CREATED_DATE -> TB_VEHICLE.CREATED_DATE
		LAST_MODIFIED_DATE -> TB_VEHICLE.LAST_MODIFIED_DATE
	}

	// similarityScore sorts first (a no-op constant when there's no text search — Postgres accepts
	// ordering by a constant), TB_VEHICLE.BRAND always stays last as the final tiebreaker (v1's sole,
	// implicit order), so pagination stays deterministic whether or not a v2 caller requested a sort,
	// and a caller sorting by BRAND itself just repeats the same key harmlessly.
	private fun orderFields(
		similarityScore: Field<Float>,
		sortFields: List<SortModel<VehicleSortFieldEnum>>,
	): List<OrderField<*>> {
		val fields = mutableListOf<OrderField<*>>(similarityScore.desc())
		sortFields.forEach { fields += if (it.direction == DESC) it.field.toJooqField().desc() else it.field.toJooqField().asc() }
		fields += TB_VEHICLE.BRAND
		return fields
	}

	fun findAll(
		projectId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		presenceSearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
		sortFields: List<SortModel<VehicleSortFieldEnum>> = emptyList(),
		limit: Int,
		offset: Int,
	): Flux<VehicleEntity> {
		val lastMovement = lastMovementCte()
		val lastMovementType = lastMovementTypeField()
		val lastMovementDateTime = lastMovementDateTimeField()
		val creator = creatorTable()
		val editor = editorTable()
		val project = projectTable()
		val similarityScore = (if (textSearched == null) DSL.inline(1f) else similarity(
			TB_VEHICLE.SEARCH_TEXT,
			DSL.`val`(textSearched)
		)).`as`("similarity_score")
		val fullCount = count().over().`as`("full_count")
		return Flux.from(
			dsl.with(lastMovement)
				.select(
					TB_VEHICLE.asterisk(),
					lastMovementType,
					lastMovementDateTime,
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
				.from(TB_VEHICLE)
				.leftJoin(lastMovement).on(lastMovementVehicleIdField().eq(TB_VEHICLE.ID))
				.join(project).on(TB_VEHICLE.PROJECT_ID.eq(project.ID).and(project.VISIBLE.isTrue))
				.leftJoin(creator).on(TB_VEHICLE.CREATED_BY.eq(creator.ID))
				.leftJoin(editor).on(TB_VEHICLE.LAST_MODIFIED_BY.eq(editor.ID))
				.where(searchConditions(projectId, textSearched, visibilitySearched, availabilitySearched, presenceSearched, dateTimeSearched, lastMovementType))
				.orderBy(orderFields(similarityScore, sortFields))
				.limit(limit).offset(offset)
		).map { it.toEntity(lastMovementType, lastMovementDateTime, creator, editor, project, fullCount) }
	}

	fun countAll(
		projectId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		presenceSearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
	): Mono<Long> {
		val lastMovement = lastMovementCte()
		val lastMovementType = lastMovementTypeField()
		return Mono.from(
			dsl.with(lastMovement)
				.select(count(TB_VEHICLE.ID))
				.from(TB_VEHICLE)
				.leftJoin(lastMovement).on(lastMovementVehicleIdField().eq(TB_VEHICLE.ID))
				.where(
					searchConditions(
						projectId,
						textSearched,
						visibilitySearched,
						availabilitySearched,
						presenceSearched,
						dateTimeSearched,
						lastMovementType
					)
				)
		).map { it.value1().toLong() }
	}

	fun findAllByIds(projectId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<VehicleEntity> {
		if (ids.isEmpty()) return Flux.empty()
		val lastMovement = lastMovementCte()
		val lastMovementType = lastMovementTypeField()
		val lastMovementDateTime = lastMovementDateTimeField()
		val creator = creatorTable()
		val editor = editorTable()
		val project = projectTable()
		return Flux.from(
			dsl.with(lastMovement)
				.select(
					TB_VEHICLE.asterisk(),
					lastMovementType,
					lastMovementDateTime,
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
				.from(TB_VEHICLE)
				.leftJoin(lastMovement).on(lastMovementVehicleIdField().eq(TB_VEHICLE.ID))
				.join(project).on(TB_VEHICLE.PROJECT_ID.eq(project.ID).and(project.VISIBLE.isTrue))
				.leftJoin(creator).on(TB_VEHICLE.CREATED_BY.eq(creator.ID))
				.leftJoin(editor).on(TB_VEHICLE.LAST_MODIFIED_BY.eq(editor.ID))
				.where(
					TB_VEHICLE.PROJECT_ID.eq(projectId).and(TB_VEHICLE.ID.`in`(ids))
						.and(visibleCondition(TB_VEHICLE.VISIBLE, visibilitySearched))
				)
				.orderBy(TB_VEHICLE.BRAND)
		).map { it.toEntity(lastMovementType, lastMovementDateTime, creator, editor, project) }
	}

	fun findWithLimit(
		projectId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		presenceSearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
		limit: Int,
	): Flux<VehicleEntity> {
		val lastMovement = lastMovementCte()
		val lastMovementType = lastMovementTypeField()
		val lastMovementDateTime = lastMovementDateTimeField()
		val creator = creatorTable()
		val editor = editorTable()
		val project = projectTable()
		val similarityScore = (if (textSearched == null) DSL.inline(1f) else similarity(
			TB_VEHICLE.SEARCH_TEXT,
			DSL.`val`(textSearched)
		)).`as`("similarity_score")
		return Flux.from(
			dsl.with(lastMovement)
				.select(
					TB_VEHICLE.asterisk(),
					lastMovementType,
					lastMovementDateTime,
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
				.from(TB_VEHICLE)
				.leftJoin(lastMovement).on(lastMovementVehicleIdField().eq(TB_VEHICLE.ID))
				.join(project).on(TB_VEHICLE.PROJECT_ID.eq(project.ID).and(project.VISIBLE.isTrue))
				.leftJoin(creator).on(TB_VEHICLE.CREATED_BY.eq(creator.ID))
				.leftJoin(editor).on(TB_VEHICLE.LAST_MODIFIED_BY.eq(editor.ID))
				.where(
					searchConditions(
						projectId,
						textSearched,
						visibilitySearched,
						availabilitySearched,
						presenceSearched,
						dateTimeSearched,
						lastMovementType
					)
				)
				.orderBy(similarityScore.desc(), TB_VEHICLE.BRAND)
				.limit(limit)
		).map { it.toEntity(lastMovementType, lastMovementDateTime, creator, editor, project) }
	}

	fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<VehicleEntity> {
		val lastMovement = lastMovementCte()
		val lastMovementType = lastMovementTypeField()
		val lastMovementDateTime = lastMovementDateTimeField()
		val creator = creatorTable()
		val editor = editorTable()
		val project = projectTable()
		return Mono.from(
			dsl.with(lastMovement)
				.select(
					TB_VEHICLE.asterisk(),
					lastMovementType,
					lastMovementDateTime,
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
				.from(TB_VEHICLE)
				.leftJoin(lastMovement).on(lastMovementVehicleIdField().eq(TB_VEHICLE.ID))
				.join(project).on(TB_VEHICLE.PROJECT_ID.eq(project.ID).and(project.VISIBLE.isTrue))
				.leftJoin(creator).on(TB_VEHICLE.CREATED_BY.eq(creator.ID))
				.leftJoin(editor).on(TB_VEHICLE.LAST_MODIFIED_BY.eq(editor.ID))
				.where(
					TB_VEHICLE.PROJECT_ID.eq(projectId).and(TB_VEHICLE.ID.eq(id))
						.and(visibleCondition(TB_VEHICLE.VISIBLE, visibilitySearched))
				)
		).map { it.toEntity(lastMovementType, lastMovementDateTime, creator, editor, project) }
	}

	fun findUnusedSince(dateThreshold: LocalDate): Flux<UUID> {
		val lastMovement = lastMovementCte()
		val lastMovementDateTime = lastMovementDateTimeField()
		val thresholdAsTimestamp = DSL.`val`(dateThreshold).cast(TB_VEHICLE.LAST_MODIFIED_DATE)
		return Flux.from(
			dsl.with(lastMovement)
				.select(TB_VEHICLE.ID)
				.from(TB_VEHICLE)
				.leftJoin(lastMovement).on(lastMovementVehicleIdField().eq(TB_VEHICLE.ID))
				.where(
					lastMovementDateTime.isNull.or(lastMovementDateTime.lt(thresholdAsTimestamp))
						.and(TB_VEHICLE.LAST_MODIFIED_DATE.lt(thresholdAsTimestamp))
				)
		).map { it.value1()!! }
	}

	fun save(entity: VehicleEntity): Mono<VehicleEntity> = if (entity.id == null) insert(entity) else update(entity)

	private fun insert(entity: VehicleEntity): Mono<VehicleEntity> {
		val record = dsl.newRecord(TB_VEHICLE)
		record.setGeneric(entity, columns)
		record.setGenericProject(entity, TB_VEHICLE.PROJECT_ID)
		entity.licensePlate?.let { record.set(TB_VEHICLE.LICENSE_PLATE, it) }
		entity.brand?.let { record.set(TB_VEHICLE.BRAND, it) }
		entity.model?.let { record.set(TB_VEHICLE.MODEL, it) }
		entity.startAvailabilityDate?.let { record.set(TB_VEHICLE.START_AVAILABILITY_DATE, it) }
		entity.startAvailabilityTime?.let { record.set(TB_VEHICLE.START_AVAILABILITY_TIME, it) }
		entity.endAvailabilityDate?.let { record.set(TB_VEHICLE.END_AVAILABILITY_DATE, it) }
		entity.endAvailabilityTime?.let { record.set(TB_VEHICLE.END_AVAILABILITY_TIME, it) }
		return Mono.from(dsl.insertInto(TB_VEHICLE).set(record).returning()).map { it.toEntity() }
	}

	private fun update(entity: VehicleEntity): Mono<VehicleEntity> = Mono.from(
		dsl.update(TB_VEHICLE)
			.setGeneric(entity, columns)
			.setGenericProject(entity, TB_VEHICLE.PROJECT_ID)
			.set(TB_VEHICLE.LICENSE_PLATE, entity.licensePlate)
			.set(TB_VEHICLE.BRAND, entity.brand)
			.set(TB_VEHICLE.MODEL, entity.model)
			.set(TB_VEHICLE.START_AVAILABILITY_DATE, entity.startAvailabilityDate)
			.set(TB_VEHICLE.START_AVAILABILITY_TIME, entity.startAvailabilityTime)
			.set(TB_VEHICLE.END_AVAILABILITY_DATE, entity.endAvailabilityDate)
			.set(TB_VEHICLE.END_AVAILABILITY_TIME, entity.endAvailabilityTime)
			.where(TB_VEHICLE.ID.eq(entity.id))
			.returning()
	).map { it.toEntity() }

	fun deleteById(id: UUID): Mono<Unit> = Mono.from(dsl.deleteFrom(TB_VEHICLE).where(TB_VEHICLE.ID.eq(id))).map { }

	private fun Record.toEntity(
		lastMovementType: Field<String>? = null,
		lastMovementDateTime: Field<ZonedDateTime>? = null,
		creator: TbUser? = null,
		editor: TbUser? = null,
		project: TbProject? = null,
		fullCount: Field<Int>? = null,
	): VehicleEntity = VehicleEntity(
		licensePlate = get(TB_VEHICLE.LICENSE_PLATE),
		brand = get(TB_VEHICLE.BRAND),
		model = get(TB_VEHICLE.MODEL),
		lastMovementType = lastMovementType?.let { get(it) }?.let { MovementTypeEnum.valueOf(it) },
		lastMovementDateTime = lastMovementDateTime?.let { get(it) },
		startAvailabilityDate = get(TB_VEHICLE.START_AVAILABILITY_DATE),
		startAvailabilityTime = get(TB_VEHICLE.START_AVAILABILITY_TIME),
		endAvailabilityDate = get(TB_VEHICLE.END_AVAILABILITY_DATE),
		endAvailabilityTime = get(TB_VEHICLE.END_AVAILABILITY_TIME),
	).apply {
		fillGeneric(this, columns, creator, editor, fullCount)
		fillGenericProject(this, TB_VEHICLE.PROJECT_ID, project)
	}
}
