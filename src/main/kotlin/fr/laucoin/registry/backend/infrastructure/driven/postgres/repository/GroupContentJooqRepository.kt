package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupContentEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_GROUP
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_GROUP_CONTENT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_PARTICIPANT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.activeNowCondition
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.visibleCondition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
class GroupContentJooqRepository(private val dsl: DSLContext) {
	fun findAllByGroupIds(
		projectId: UUID,
		groupIds: List<UUID>,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
	): Flux<GroupContentEntity> =
		findAllByGroupIds(projectId, groupIds, visibilitySearched, availabilitySearched, dsl)

	fun findAllByGroupIds(
		projectId: UUID,
		groupIds: List<UUID>,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		using: DSLContext,
	): Flux<GroupContentEntity> {
		val conditions = mutableListOf(
			TB_GROUP.PROJECT_ID.eq(projectId),
			TB_GROUP_CONTENT.GROUP_ID.`in`(groupIds),
			visibleCondition(TB_PARTICIPANT.VISIBLE, visibilitySearched),
		)
		availabilitySearched?.let {
			val isAvailable = activeNowCondition(
				TB_PARTICIPANT.START_AVAILABILITY_DATE,
				TB_PARTICIPANT.START_AVAILABILITY_TIME,
				TB_PARTICIPANT.END_AVAILABILITY_DATE,
				TB_PARTICIPANT.END_AVAILABILITY_TIME,
			)
			conditions += if (it) isAvailable else isAvailable.not()
		}
		return Flux.from(
			using.select(
				TB_GROUP_CONTENT.asterisk(),
				TB_PARTICIPANT.FIRST_NAME,
				TB_PARTICIPANT.LAST_NAME,
				TB_PARTICIPANT.BIRTHDAY,
				TB_PARTICIPANT.TYPE,
			)
				.from(TB_GROUP_CONTENT)
				.join(TB_GROUP).on(TB_GROUP_CONTENT.GROUP_ID.eq(TB_GROUP.ID))
				.join(TB_PARTICIPANT)
				.on(TB_GROUP_CONTENT.PARTICIPANT_ID.eq(TB_PARTICIPANT.ID).and(TB_PARTICIPANT.PURGED.isFalse))
				.where(DSL.and(conditions))
		).map { it.toEntity() }
	}

	fun save(entity: GroupContentEntity): Mono<GroupContentEntity> = save(entity, dsl)

	fun save(entity: GroupContentEntity, using: DSLContext): Mono<GroupContentEntity> {
		val record = using.newRecord(TB_GROUP_CONTENT)
		entity.groupId?.let { record.set(TB_GROUP_CONTENT.GROUP_ID, it) }
		entity.participantId?.let { record.set(TB_GROUP_CONTENT.PARTICIPANT_ID, it) }
		return Mono.from(using.insertInto(TB_GROUP_CONTENT).set(record).returning())
			.map { it.toEntity(includeParticipantInfo = false) }
	}

	fun saveAll(entities: Iterable<GroupContentEntity>): Flux<GroupContentEntity> = saveAll(entities, dsl)

	fun saveAll(entities: Iterable<GroupContentEntity>, using: DSLContext): Flux<GroupContentEntity> =
		Flux.fromIterable(entities).flatMap { save(it, using) }

	fun deleteAllByGroupIdAndParticipantIds(groupId: UUID, participantIds: List<UUID>): Mono<Unit> =
		deleteAllByGroupIdAndParticipantIds(groupId, participantIds, dsl)

	fun deleteAllByGroupIdAndParticipantIds(groupId: UUID, participantIds: List<UUID>, using: DSLContext): Mono<Unit> =
		Mono.from(
			using.deleteFrom(TB_GROUP_CONTENT)
				.where(TB_GROUP_CONTENT.GROUP_ID.eq(groupId).and(TB_GROUP_CONTENT.PARTICIPANT_ID.`in`(participantIds)))
		).map { }

	fun deleteAllByParticipantIdAndGroupIds(participantId: UUID, groupIds: List<UUID>): Mono<Unit> =
		deleteAllByParticipantIdAndGroupIds(participantId, groupIds, dsl)

	fun deleteAllByParticipantIdAndGroupIds(participantId: UUID, groupIds: List<UUID>, using: DSLContext): Mono<Unit> =
		Mono.from(
			using.deleteFrom(TB_GROUP_CONTENT)
				.where(TB_GROUP_CONTENT.PARTICIPANT_ID.eq(participantId).and(TB_GROUP_CONTENT.GROUP_ID.`in`(groupIds)))
		).map { }

	private fun Record.toEntity(includeParticipantInfo: Boolean = true): GroupContentEntity = GroupContentEntity(
		id = get(TB_GROUP_CONTENT.ID),
		groupId = get(TB_GROUP_CONTENT.GROUP_ID),
		participantId = get(TB_GROUP_CONTENT.PARTICIPANT_ID),
		participantFirstName = if (includeParticipantInfo) get(TB_PARTICIPANT.FIRST_NAME) else null,
		participantLastName = if (includeParticipantInfo) get(TB_PARTICIPANT.LAST_NAME) else null,
		participantBirthday = if (includeParticipantInfo) get(TB_PARTICIPANT.BIRTHDAY) else null,
		participantType = if (includeParticipantInfo) get(TB_PARTICIPANT.TYPE) else null,
	)
}
