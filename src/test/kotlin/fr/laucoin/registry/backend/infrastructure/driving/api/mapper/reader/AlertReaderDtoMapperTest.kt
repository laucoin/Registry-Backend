package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum.REINFORCED
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class AlertReaderDtoMapperTest {
	private val statusMapper: AlertStatusReaderDtoMapper = mock()
	private val criticalityMapper: AlertCriticalityReaderDtoMapper = mock()
	private val mapper = AlertReaderDtoMapper(statusMapper, criticalityMapper)

	@Test
	fun `Should toDto expose the criticality and the last communication`() {
		// Arrange
		val dateTime = ZonedDateTime.now(ZoneOffset.UTC)
		val model = AlertModel(
			criticality = REINFORCED,
			lastCommunication = CommunicationModel(dateTime, "message"),
		)
		whenever(criticalityMapper.toDto(REINFORCED)).thenReturn(LabelDto("REINFORCED", "Reinforced"))

		// Act
		val dto = mapper.toDto(model)

		// Assert
		assertEquals(LabelDto("REINFORCED", "Reinforced"), dto.criticality)
		assertEquals(dateTime, dto.lastCommunication?.dateTime)
		assertEquals("message", dto.lastCommunication?.message)
	}

	@Test
	fun `Should toDto leave the criticality and the last communication empty when absent`() {
		// Arrange
		val model = AlertModel()

		// Act
		val dto = mapper.toDto(model)

		// Assert
		assertNull(dto.criticality)
		assertNull(dto.lastCommunication)
		verifyNoInteractions(criticalityMapper)
	}
}
