package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.DEFINITIVE_DEPARTURE
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonKindEnum.REASON
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.PlannedMovementModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReasonsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantReaderDto
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class PlannedMovementReaderDtoMapperTest {
	private val typeMapper: MovementTypeReaderDtoMapper = mock()
	private val reasonMapper: MovementReasonReaderDtoMapper = mock()
	private val participantMapper: ParticipantReaderDtoMapper = mock()
	private val mapper = PlannedMovementReaderDtoMapper(typeMapper, reasonMapper, participantMapper)

	@Test
	fun `Should toDto map an individual arrival without reason and pool name`() {
		// Arrange
		val participant = ParticipantModel()
		val participantDto = ParticipantReaderDto()
		val plannedAt = CustomDateTimeModel(LocalDate.of(2026, 7, 14))
		whenever(typeMapper.toDto(IN)).thenReturn(LabelDto("IN", "In"))
		whenever(participantMapper.toDtoList(listOf(participant))).thenReturn(listOf(participantDto))

		// Act
		val dto = mapper.toDto(PlannedMovementModel(IN, plannedAt, listOf(participant)))

		// Assert
		assertEquals(LabelDto("IN", "In"), dto.type)
		assertNull(dto.reason)
		assertNull(dto.poolName)
		assertEquals(REGISTERED, dto.contentType)
		assertEquals(plannedAt, dto.plannedAt)
		assertEquals(listOf(participantDto), dto.participants)
	}

	@Test
	fun `Should toDto map a group departure with the definitive departure reason and the pool name`() {
		// Arrange
		val members = listOf(ParticipantModel(), ParticipantModel())
		val reason = MovementReasonsReaderDto(label = "Definitive departure", value = "DEFINITIVE_DEPARTURE", type = OUT, kind = REASON)
		whenever(typeMapper.toDto(OUT)).thenReturn(LabelDto("OUT", "Out"))
		whenever(reasonMapper.toDto(DEFINITIVE_DEPARTURE)).thenReturn(reason)
		whenever(participantMapper.toDtoList(members)).thenReturn(listOf(ParticipantReaderDto(), ParticipantReaderDto()))

		// Act
		val dto = mapper.toDto(PlannedMovementModel(OUT, null, members, "Alpha"))

		// Assert
		assertEquals(reason, dto.reason)
		assertEquals("Alpha", dto.poolName)
		assertNull(dto.plannedAt)
		assertEquals(2, dto.participants.size)
	}
}
