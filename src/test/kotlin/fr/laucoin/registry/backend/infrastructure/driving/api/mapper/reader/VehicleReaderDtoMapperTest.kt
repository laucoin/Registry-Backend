package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.IN
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.StatusLabelMappingHelper
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class VehicleReaderDtoMapperTest {
	private val statusHelper: StatusLabelMappingHelper = mock()
	private val mapper: VehicleReaderDtoMapper = VehicleReaderDtoMapperImpl(statusHelper)

	private fun vehicle() = VehicleModel(
		licensePlate = "AB-123-CD",
		brand = "Renault",
		model = "Trafic",
		status = IN,
		startAvailability = CustomDateTimeModel.MIN,
		endAvailability = CustomDateTimeModel.MAX,
	).apply {
		id = UUID.randomUUID()
		visible = true
		creation = HistoryModel(ZonedDateTime.now(ZoneOffset.UTC))
		lastEdition = HistoryModel(ZonedDateTime.now(ZoneOffset.UTC))
	}

	@Test
	fun `Should toDto convert VehicleModel with the status label of the helper`() {
		// Arrange
		val vehicle = vehicle()
		val status = LabelDto("IN", "In since 2 hours")
		whenever(statusHelper.vehicleStatus(any())).thenReturn(status)

		// Act
		val result = mapper.toDto(vehicle)

		// Assert
		assertEquals(vehicle.id, result.id)
		assertEquals(vehicle.licensePlate, result.licensePlate)
		assertEquals(vehicle.brand, result.brand)
		assertEquals(vehicle.model, result.model)
		assertEquals(status, result.status)
		assertEquals(vehicle.startAvailability, result.startAvailability)
		assertEquals(vehicle.endAvailability, result.endAvailability)
		assertEquals(vehicle.visible, result.visible)
		assertEquals(vehicle.creation, result.creation)
		assertEquals(vehicle.lastEdition, result.lastEdition)
		verify(statusHelper).vehicleStatus(vehicle)
	}

	@Test
	fun `Should toDto leave the status empty when the helper gives none`() {
		// Arrange
		val vehicle = VehicleModel()
		whenever(statusHelper.vehicleStatus(any())).thenReturn(null)

		// Act
		val result = mapper.toDto(vehicle)

		// Assert
		assertNull(result.status)
		assertNull(result.licensePlate)
	}
}
