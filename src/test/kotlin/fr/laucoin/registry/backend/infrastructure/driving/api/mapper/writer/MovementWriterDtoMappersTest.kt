package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.GUEST
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.GuestMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.MovementContentWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ParticipantMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ParticipantMovementWriterDto.ParticipantMovementContentWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.IdReferenceDtoMapperImpl
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import java.time.ZonedDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MovementWriterDtoMappersTest {
	private val ids = IdReferenceDtoMapperImpl()
	private val guestMapper = GuestMovementWriterDtoMapperImpl(MovementContentWriterDtoMapperImpl(ids))
	private val participantMapper = ParticipantMovementWriterDtoMapperImpl(
		ParticipantMovementContentWriterDtoMapperImpl(ids),
		ids,
	)
	private val dateTime = ZonedDateTime.parse("2026-07-01T09:00:00Z")

	@Test
	fun `Should map a guest movement with the guest content type and its participants`() {
		// Arrange
		val participantId = UUID.randomUUID()
		val dto = GuestMovementWriterDto(dateTime, IN, null, listOf(MovementContentWriterDto(participantId)), emptyList())

		// Act
		val result = guestMapper.toModel(dto, defaultProjectId)

		// Assert
		assertEquals(GUEST, result.contentType)
		assertEquals(dateTime, result.dateTime)
		assertEquals(IN, result.type)
		assertEquals(participantId, result.content.single().participant?.id)
		assertEquals(defaultProjectId, result.projectId)
	}

	@Test
	fun `Should map a registered movement with its activity and its content`() {
		// Arrange
		val activityId = UUID.randomUUID()
		val participantId = UUID.randomUUID()
		val vehicleId = UUID.randomUUID()
		val dto = ParticipantMovementWriterDto(
			dateTime = dateTime,
			type = IN,
			reason = MovementReasonEnum.entries.first(),
			activityId = activityId,
			content = listOf(
				ParticipantMovementContentWriterDto(vehicleId = vehicleId, poolName = "Pool", participantId = participantId),
			),
		)

		// Act
		val result = participantMapper.toModel(dto, defaultProjectId)

		// Assert
		assertEquals(REGISTERED, result.contentType)
		assertEquals(activityId, result.activity?.id)
		val content = result.content.single()
		assertEquals("Pool", content.poolName)
		assertEquals(participantId, content.participant?.id)
		assertEquals(vehicleId, content.vehicle?.id)
	}

	@Test
	fun `Should leave the activity and the vehicle empty when they are not given`() {
		// Arrange
		val dto = ParticipantMovementWriterDto(
			dateTime = dateTime,
			type = IN,
			reason = null,
			activityId = null,
			content = listOf(ParticipantMovementContentWriterDto(participantId = UUID.randomUUID())),
		)

		// Act
		val result = participantMapper.toModel(dto, defaultProjectId)

		// Assert
		assertNull(result.activity)
		assertNull(result.content.single().vehicle)
	}
}
