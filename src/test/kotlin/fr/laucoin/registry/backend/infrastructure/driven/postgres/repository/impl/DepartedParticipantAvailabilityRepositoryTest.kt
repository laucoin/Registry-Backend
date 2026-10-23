package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.SHOPPING
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class DepartedParticipantAvailabilityRepositoryTest : TestContext() {
	@Autowired
	private lateinit var participantPort: IParticipantPort

	@Autowired
	private lateinit var groupPort: IGroupPort

	@Autowired
	private lateinit var movementPort: IMovementPort

	@Autowired
	private lateinit var databaseClient: DatabaseClient

	private lateinit var groupId: UUID
	private lateinit var insideId: UUID
	private lateinit var outsideId: UUID
	private lateinit var outMovementId: UUID
	private val pageable = PageableModel(0, 200)

	@Test
	@Order(1)
	fun `Should create a group with an inside and an outside participant inside their window`() {
		// Arrange
		val group = GroupModel().apply {
			name = "departed group"
			this.projectId = defaultProjectId
			create(currentUser())
		}

		// Act
		groupId = groupPort.create(group).block()!!.id!!
		insideId = createParticipant("inside")
		outsideId = createParticipant("outside")
		createMovement(insideId, IN, reason = null)
		outMovementId = createMovement(outsideId, OUT, reason = SHOPPING)

		// Assert
		assertTrue(listOf(groupId, insideId, outsideId, outMovementId).isNotEmpty())
	}

	@Test
	@Order(2)
	fun `Should keep the participants inside their window available before their departure`() {
		// Act
		val available = findAvailableIds()
		val group = groupPort.findByIdWithContent(defaultProjectId, groupId, null, null, null).block()!!
		val content = groupPort.findContent(defaultProjectId, listOf(groupId), true, true).collectList().block()!!
		val current = movementPort.findCurrentContent(defaultProjectId, listOf(outMovementId)).collectList().block()!!

		// Assert
		assertTrue(available.containsAll(listOf(insideId, outsideId)))
		assertEquals(1, group.insideMembersCount)
		assertEquals(setOf(insideId, outsideId), content.flatMap { it.second }.mapNotNull { it.id }.toSet())
		assertEquals(listOf(outsideId), current.flatMap { it.second }.mapNotNull { it.participant?.id })
	}

	@Test
	@Order(3)
	fun `Should exclude the departed participants from every availability predicate`() {
		// Arrange
		markAsDeparted(insideId, outsideId)

		// Act
		val available = findAvailableIds()
		val group = groupPort.findByIdWithContent(defaultProjectId, groupId, null, null, null).block()!!
		val content = groupPort.findContent(defaultProjectId, listOf(groupId), true, true).collectList().block()!!
		val current = movementPort.findCurrentContent(defaultProjectId, listOf(outMovementId)).collectList().block()!!

		// Assert
		assertTrue(available.none { it == insideId || it == outsideId })
		assertEquals(0, group.insideMembersCount)
		assertTrue(content.flatMap { it.second }.none { it.id == insideId || it.id == outsideId })
		assertTrue(current.flatMap { it.second }.isEmpty())
	}

	@Test
	@Order(4)
	fun `Should keep the departed participants on a date before their departure`() {
		// Arrange
		val beforeDeparture = ZonedDateTime.now().minusHours(1)
		val afterDeparture = ZonedDateTime.now().plusMinutes(1)

		// Act
		val before = findIdsAt(beforeDeparture)
		val after = findIdsAt(afterDeparture)

		// Assert
		assertTrue(before.containsAll(listOf(insideId, outsideId)))
		assertTrue(after.none { it == insideId || it == outsideId })
	}

	@AfterAll
	fun cleanup() {
		val ids = listOf(insideId, outsideId)
		execute("DELETE FROM tb_movement WHERE id IN (SELECT movement_id FROM tb_movement_content WHERE participant_id IN (:ids))", ids)
		execute("DELETE FROM tb_participant WHERE id IN (:ids)", ids)
		databaseClient.sql("DELETE FROM tb_group WHERE id = :id").bind("id", groupId).then().block()
	}

	private fun execute(sql: String, ids: List<UUID>) {
		databaseClient.sql(sql).bind("ids", ids).then().block()
	}

	private fun findAvailableIds(): List<UUID> {
		return participantPort.findPage(defaultProjectId, pageable, ParticipantSearchParamModel(isAvailable = true))
			.block()!!.content.mapNotNull { it.id }
	}

	private fun findIdsAt(at: ZonedDateTime): List<UUID> {
		return participantPort.findPage(defaultProjectId, pageable, ParticipantSearchParamModel(query = null, at = at))
			.block()!!.content.mapNotNull { it.id }
	}

	private fun markAsDeparted(vararg ids: UUID) {
		ids.forEach {
			databaseClient.sql("UPDATE tb_participant SET effective_departure = NOW() - INTERVAL '10 minutes' WHERE id = :id")
				.bind("id", it)
				.fetch().rowsUpdated().block()
		}
	}

	private fun createParticipant(lastName: String): UUID {
		val participant = ParticipantModel().apply {
			firstName = "departed"
			this.lastName = lastName
			birthday = LocalDate.EPOCH
			type = REGISTERED
			startAvailability = CustomDateTimeModel(LocalDate.now().minusDays(1))
			endAvailability = CustomDateTimeModel(LocalDate.now().plusDays(1))
			groups = listOf(GroupModel().apply { id = groupId })
			this.projectId = defaultProjectId
			create(currentUser())
		}
		return participantPort.create(participant).block()!!.id!!
	}

	private fun createMovement(
		participantId: UUID,
		movementType: MovementTypeEnum,
		reason: MovementReasonEnum?,
	): UUID {
		val movement = MovementModel(contentType = REGISTERED).apply {
			dateTime = ZonedDateTime.now().minusHours(2)
			type = movementType
			this.reason = reason
			content =
				listOf(MovementContentModel().apply { participant = ParticipantModel().apply { id = participantId } })
			this.projectId = defaultProjectId
			create(currentUser())
		}
		return movementPort.create(movement).block()!!.id!!
	}
}
