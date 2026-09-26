package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementContentEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_GROUP
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_GROUP_CONTENT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_MOVEMENT_CONTENT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_PARTICIPANT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_VEHICLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.activeNowCondition
import org.jooq.CommonTableExpression
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.max
import org.jooq.impl.DSL.name
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.UUID

@Repository
class MovementContentJooqRepository(private val dsl: DSLContext) {
	private fun lastParticipantMovementCte(): CommonTableExpression<*> {
		val plm = dsl.select(
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

	fun findAllByMovementIds(projectId: UUID, movementIds: List<UUID>): Flux<MovementContentEntity> =
		findAllByMovementIds(projectId, movementIds, dsl)

	fun findAllByMovementIds(projectId: UUID, movementIds: List<UUID>, using: DSLContext): Flux<MovementContentEntity> {
		if (movementIds.isEmpty()) return Flux.empty()
		return Flux.from(
			using.select(
				TB_MOVEMENT_CONTENT.asterisk(),
				TB_PARTICIPANT.FIRST_NAME, TB_PARTICIPANT.LAST_NAME, TB_PARTICIPANT.BIRTHDAY, TB_PARTICIPANT.TYPE,
				TB_VEHICLE.LICENSE_PLATE, TB_VEHICLE.BRAND, TB_VEHICLE.MODEL,
			)
				.from(TB_MOVEMENT_CONTENT)
				.join(TB_MOVEMENT).on(TB_MOVEMENT_CONTENT.MOVEMENT_ID.eq(TB_MOVEMENT.ID))
				.join(TB_PARTICIPANT)
				.on(TB_MOVEMENT_CONTENT.PARTICIPANT_ID.eq(TB_PARTICIPANT.ID).and(TB_PARTICIPANT.PURGED.isFalse))
				.leftJoin(TB_VEHICLE).on(TB_MOVEMENT_CONTENT.VEHICLE_ID.eq(TB_VEHICLE.ID))
				.where(TB_MOVEMENT.PROJECT_ID.eq(projectId).and(TB_MOVEMENT_CONTENT.MOVEMENT_ID.`in`(movementIds)))
		).map { it.toEntity(includeParticipantAndVehicle = true) }
	}

	fun findCurrentByMovementIds(projectId: UUID, movementIds: List<UUID>): Flux<MovementContentEntity> {
		if (movementIds.isEmpty()) return Flux.empty()
		val (lastParticipantMovement, filteredGroups, currentMovement) = currentMovementCtes(projectId)
		val cmId = field(name("current_movement", "id"), UUID::class.java)
		val cmParticipantId = field(name("current_movement", "participant_id"), UUID::class.java)
		return Flux.from(
			dsl.with(lastParticipantMovement, filteredGroups, currentMovement)
				.select(
					TB_MOVEMENT_CONTENT.asterisk(),
					TB_PARTICIPANT.FIRST_NAME, TB_PARTICIPANT.LAST_NAME, TB_PARTICIPANT.BIRTHDAY, TB_PARTICIPANT.TYPE,
					TB_VEHICLE.LICENSE_PLATE, TB_VEHICLE.BRAND, TB_VEHICLE.MODEL,
				)
				.from(currentMovement)
				.join(TB_MOVEMENT_CONTENT).on(
					TB_MOVEMENT_CONTENT.MOVEMENT_ID.eq(cmId).and(TB_MOVEMENT_CONTENT.PARTICIPANT_ID.eq(cmParticipantId))
				)
				.join(TB_MOVEMENT).on(TB_MOVEMENT_CONTENT.MOVEMENT_ID.eq(TB_MOVEMENT.ID))
				.join(TB_PARTICIPANT)
				.on(TB_MOVEMENT_CONTENT.PARTICIPANT_ID.eq(TB_PARTICIPANT.ID).and(TB_PARTICIPANT.PURGED.isFalse))
				.leftJoin(TB_VEHICLE).on(TB_MOVEMENT_CONTENT.VEHICLE_ID.eq(TB_VEHICLE.ID))
				.where(TB_PARTICIPANT.PROJECT_ID.eq(projectId).and(TB_MOVEMENT_CONTENT.MOVEMENT_ID.`in`(movementIds)))
		).map { it.toEntity(includeParticipantAndVehicle = true) }
	}

	fun save(entity: MovementContentEntity): Mono<MovementContentEntity> = save(entity, dsl)

	fun save(entity: MovementContentEntity, using: DSLContext): Mono<MovementContentEntity> {
		val record = using.newRecord(TB_MOVEMENT_CONTENT)
		entity.movementId?.let { record.set(TB_MOVEMENT_CONTENT.MOVEMENT_ID, it) }
		entity.poolName?.let { record.set(TB_MOVEMENT_CONTENT.POOL_NAME, it) }
		entity.participantId?.let { record.set(TB_MOVEMENT_CONTENT.PARTICIPANT_ID, it) }
		entity.vehicleId?.let { record.set(TB_MOVEMENT_CONTENT.VEHICLE_ID, it) }
		return Mono.from(using.insertInto(TB_MOVEMENT_CONTENT).set(record).returning()).map { it.toEntity() }
	}

	fun saveAll(entities: Iterable<MovementContentEntity>): Flux<MovementContentEntity> = saveAll(entities, dsl)

	fun saveAll(entities: Iterable<MovementContentEntity>, using: DSLContext): Flux<MovementContentEntity> =
		Flux.fromIterable(entities).flatMap { save(it, using) }

	fun deleteAllById(ids: List<UUID>): Mono<Unit> = deleteAllById(ids, dsl)

	fun deleteAllById(ids: List<UUID>, using: DSLContext): Mono<Unit> =
		Mono.from(using.deleteFrom(TB_MOVEMENT_CONTENT).where(TB_MOVEMENT_CONTENT.ID.`in`(ids))).map { }

	private fun Record.toEntity(includeParticipantAndVehicle: Boolean = false): MovementContentEntity =
		MovementContentEntity(
			id = get(TB_MOVEMENT_CONTENT.ID),
			movementId = get(TB_MOVEMENT_CONTENT.MOVEMENT_ID),
			poolName = get(TB_MOVEMENT_CONTENT.POOL_NAME),
			participantId = get(TB_MOVEMENT_CONTENT.PARTICIPANT_ID),
			participantFirstName = if (includeParticipantAndVehicle) get(TB_PARTICIPANT.FIRST_NAME) else null,
			participantLastName = if (includeParticipantAndVehicle) get(TB_PARTICIPANT.LAST_NAME) else null,
			participantBirthday = if (includeParticipantAndVehicle) get(TB_PARTICIPANT.BIRTHDAY) else null,
			participantType = if (includeParticipantAndVehicle) get(TB_PARTICIPANT.TYPE) else null,
			vehicleId = get(TB_MOVEMENT_CONTENT.VEHICLE_ID),
			vehicleLicensePlate = if (includeParticipantAndVehicle) get(TB_VEHICLE.LICENSE_PLATE) else null,
			vehicleBrand = if (includeParticipantAndVehicle) get(TB_VEHICLE.BRAND) else null,
			vehicleModel = if (includeParticipantAndVehicle) get(TB_VEHICLE.MODEL) else null,
		)
}
