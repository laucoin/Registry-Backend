package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class VehicleReaderDtoMapperTest {
    private val eventMapper: EventReaderDtoMapper = Mockito.mock()
    private val mapper: VehicleReaderDtoMapper = VehicleReaderDtoMapper(eventMapper)

    @Test
    fun `Should toDto convert VehicleModel to VehicleReaderDto`() {
        // Arrange
        val vehicle = VehicleModel().apply {
            id = UUID.randomUUID()
            event = EventModel()
            registration = "AB-123-CD"
            brand = "Toyota"
            model = "Hilux"
            begin = ZonedDateTime.now()
            end = ZonedDateTime.now().plusDays(1)
            visible = true
            creation = HistoryModel()
            lastEdition = HistoryModel()
        }

        // Act
        val result = mapper.toDto(vehicle, Locale.getDefault())

        // Assert
        verify(eventMapper, times(1)).toDto(vehicle.event !!, Locale.getDefault())

        assertEquals(vehicle.id, result.id)
        assertEquals(vehicle.registration, result.registration)
        assertEquals(vehicle.brand, result.brand)
        assertEquals(vehicle.model, result.model)
        assertEquals(vehicle.begin, result.begin)
        assertEquals(vehicle.end, result.end)
        assertEquals(vehicle.visible, result.visible)
        assertEquals(vehicle.creation, result.creation)
        assertEquals(vehicle.lastEdition, result.lastEdition)
    }
}
