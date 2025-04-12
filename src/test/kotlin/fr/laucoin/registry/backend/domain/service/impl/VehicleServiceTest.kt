package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_DELETE_HAS_MOVEMENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.repository.IMovementModelRepository
import fr.laucoin.registry.backend.domain.repository.IVehicleModelRepository
import fr.laucoin.registry.backend.domain.service.IEventService
import fr.laucoin.registry.backend.domain.service.IVehicleService
import fr.laucoin.registry.backend.test.ModelExt.eventId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import reactor.core.Exceptions
import reactor.core.publisher.Mono

class VehicleServiceTest {
    private val repository: IVehicleModelRepository = mock()
    private val eventService: IEventService = mock()
    private val movementRepository: IMovementModelRepository = mock()
    private val service: IVehicleService = VehicleService(repository, eventService, movementRepository)

    @Test
    fun `Should findVehiclesPage call repository findPage`() {
        // Arrange
        val pageable = PageableModel(0, 10)
        val params = VehicleSearchParamModel()
        whenever(repository.findPage(any(), any(), any())).thenReturn(
            Mono.just(PageModel(1, 2, 3, 4, emptyList()))
        )

        // Act
        service.findVehiclesPage(eventId, pageable, params).block()

        // Assert
        verify(repository).findPage(eventId, pageable, params)
    }

    @Test
    fun `Should findVehicleById call repository findById`() {
        // Arrange
        val vehicle = VehicleModel().apply { event = EventModel().apply { id = eventId } }
        val uuid = UUID.randomUUID()
        val onlyVisible = true
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(vehicle))

        // Act
        service.findVehicleById(eventId, uuid, onlyVisible).block()

        // Assert
        verify(repository).findById(eventId, uuid, onlyVisible)
    }

    @Test
    fun `Should findVehicleById call repository findById throw on empty result`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val onlyVisible = true
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.empty())

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.findVehicleById(eventId, uuid, onlyVisible).block()
        }) as RegistryException

        // Assert
        assertEquals(NOT_FOUND, result.status)
        assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
        assertEquals(1, result.args?.size)
        verify(repository).findById(eventId, uuid, onlyVisible)
    }

    @Test
    fun `Should findVehicleMovementsPage call repository findPageByVehicleId`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val pageable = PageableModel(0, 10)
        val params = MovementSearchParamModel(typeSearched = MovementTypeEnum.IN)
        whenever(movementRepository.findPageByVehicleId(any(), any(), any(), any())).thenReturn(
            Mono.just(PageModel(1, 2, 3, 4, emptyList()))
        )

        // Act
        service.findVehicleMovementsPage(eventId, uuid, pageable, params).block()

        // Assert
        verify(movementRepository).findPageByVehicleId(eventId, uuid, pageable, params)
    }

    @Test
    fun `Should createVehicle check date and call repository create`() {
        // Arrange
        val vehicle = VehicleModel().apply { event = EventModel().apply { id = eventId } }
        whenever(eventService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any())).thenReturn(Mono.just(eventId))
        whenever(repository.create(any())).thenReturn(Mono.just(vehicle))

        // Act
        service.createVehicle(currentUser(), vehicle).block()

        // Assert
        verify(eventService).validateDateTimes(
            eventId,
            start = null,
            end = null,
            VEHICLE_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE
        )
        verify(repository).create(vehicle)
    }

    @Test
    fun `Should updateVehicleById check date, check existing vehicle, call repository updateVehicle`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val vehicle = VehicleModel().apply { id = uuid; event = EventModel().apply { id = eventId } }
        whenever(eventService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any())).thenReturn(Mono.just(eventId))
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(vehicle))
        whenever(repository.update(any())).thenReturn(Mono.just(vehicle))

        // Act
        service.updateVehicleById(currentUser(), eventId, uuid, vehicle).block()

        // Assert
        verify(eventService).validateDateTimes(
            eventId,
            start = null,
            end = null,
            VEHICLE_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE
        )
        verify(repository).findById(eventId, uuid, visibilitySearched = null)
        verify(repository).update(vehicle)
    }

    @Test
    fun `Should disableVehicleById call existing vehicle and call repository update`() {
        // Arrange
        val vehicle = VehicleModel().apply { event = EventModel().apply { id = eventId }; visible = true }
        val uuid = UUID.randomUUID()
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(vehicle))
        whenever(repository.update(any())).thenReturn(Mono.just(vehicle))

        // Act
        service.disableVehicleById(currentUser(), eventId, uuid).block()

        // Assert
        verify(repository).findById(eventId, uuid, visibilitySearched = true)
        verify(repository).update(vehicle.apply { visible = false })
    }

    @Test
    fun `Should enableVehicleById call existing vehicle and call repository update`() {
        // Arrange
        val vehicle = VehicleModel().apply { event = EventModel().apply { id = eventId }; visible = false }
        val uuid = UUID.randomUUID()
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(vehicle))
        whenever(repository.update(any())).thenReturn(Mono.just(vehicle))

        // Act
        service.enableVehicleById(currentUser(), eventId, uuid).block()

        // Assert
        verify(repository).findById(eventId, uuid, visibilitySearched = false)
        verify(repository).update(vehicle.apply { visible = true })
    }

    @Test
    fun `Should deleteVehicleById call existing vehicle, check no movement, and call repository deleteById`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val vehicle = VehicleModel().apply { id = uuid; event = EventModel().apply { id = eventId } }
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(vehicle))
        whenever(movementRepository.countAllByVehicleId(any(), any())).thenReturn(Mono.just(0))
        whenever(repository.deleteById(any())).thenReturn(Mono.empty())

        // Act
        service.deleteVehicleById(currentUser(), eventId, uuid).block()

        // Assert
        verify(repository).findById(eventId, uuid, visibilitySearched = null)
        verify(movementRepository).countAllByVehicleId(eventId, uuid)
        verify(repository).deleteById(uuid)
    }

    @Test
    fun `Should deleteVehicleById call existing vehicle, throw if movements are linked`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val vehicle = VehicleModel().apply { id = uuid; event = EventModel().apply { id = eventId } }
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(vehicle))
        whenever(movementRepository.countAllByVehicleId(any(), any())).thenReturn(Mono.just(1))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.deleteVehicleById(currentUser(), eventId, uuid).block()
        }) as RegistryException

        // Assert
        assertEquals(FORBIDDEN, result.status)
        assertEquals(VEHICLE_DELETE_HAS_MOVEMENT, result.message)
        verify(repository).findById(eventId, uuid, visibilitySearched = null)
        verify(movementRepository).countAllByVehicleId(eventId, uuid)
        verify(repository, never()).deleteById(any())
    }
}
