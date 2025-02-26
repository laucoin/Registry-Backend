package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_DELETE_HAS_MOVEMENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.repository.IMovementModelRepository
import fr.laucoin.registry.backend.domain.repository.IVehicleModelRepository
import fr.laucoin.registry.backend.domain.service.IEventService
import fr.laucoin.registry.backend.domain.service.IVehicleService
import fr.laucoin.registry.backend.test.ModelExt.eventId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.test.util.ReflectionTestUtils.setField
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class VehicleServiceTest {
    private val repository: IVehicleModelRepository = mock()
    private val eventService: IEventService = mock()
    private val movementRepository: IMovementModelRepository = mock()
    private val service: IVehicleService = VehicleService(repository, eventService, movementRepository)

    companion object {
        private val vehicle0 = VehicleModel().apply {
            registration = "0"; brand = "Toyota"; model = "Hilux"; event = EventModel().apply { id = eventId }
        }
        private val vehicle1 = VehicleModel().apply {
            registration = "1"; brand = "Toyota"; model = "Hilux"; event = EventModel().apply { id = eventId }
        }
        private val vehicle2 = VehicleModel().apply {
            registration = "2"; brand = "Toyota"; model = "Hilux"; event = EventModel().apply { id = eventId }
        }
        private val vehicle3 = VehicleModel().apply {
            registration = "3"; brand = "Toyota"; model = "Hilux"; event = EventModel().apply { id = eventId }
        }

        private val vehicles = arrayOf(vehicle0, vehicle1, vehicle2, vehicle3)

        @JvmStatic
        fun `Should findVehiclesByEventId return Event's Vehicles`(): Stream<Arguments> = Stream.of(
            Arguments.of(ASC, null, vehicles.toList()),
            Arguments.of(DESC, null, vehicles.toList().reversed()),
            Arguments.of(ASC, "0", listOf(vehicle0)),
            Arguments.of(ASC, "1", listOf(vehicle1)),
            Arguments.of(ASC, "2", listOf(vehicle2)),
            Arguments.of(ASC, "3", listOf(vehicle3)),
            Arguments.of(DESC, "0", listOf(vehicle0)),
            Arguments.of(DESC, "1", listOf(vehicle1)),
            Arguments.of(DESC, "2", listOf(vehicle2)),
            Arguments.of(DESC, "3", listOf(vehicle3)),
            Arguments.of(ASC, "QWERTY", emptyList<VehicleModel>()),
            Arguments.of(DESC, "QWERTY", emptyList<VehicleModel>()),
        )
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findVehiclesByEventId return Event's Vehicles`(
        order: Direction,
        searched: String?,
        expectedList: List<VehicleModel>,
    ) {
        // Arrange
        setField(service, "searchThreshold", 0.5)
        `when`(repository.findAll(any(), any(), any(), anyOrNull(), anyOrNull())).thenReturn(Flux.just(*vehicles))

        // Act
        val result = service.findVehiclesByEventId(
            eventId,
            order,
            onlyVisible = true,
            onlyPresent = true,
            searched,
            startDateTime = null,
            endDateTime = null
        ).collectList().block()

        // Assert
        assertEquals(expectedList.size, result?.size)
        expectedList.forEachIndexed { index, it ->
            assertEquals(it, result?.get(index))
        }
    }

    @Test
    fun `Should findVehicleById return the Vehicle`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(vehicle0))

        // Act
        service.findVehicleById(eventId, uuid, onlyVisible = true).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = true)
    }

    @Test
    fun `Should findVehicleMovements return Flux of Movements`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val movement1 = MovementModel().apply { content = listOf(MovementContentModel()) }
        val movement2 = MovementModel().apply {
            content = listOf(MovementContentModel().apply { vehicle = VehicleModel().apply { id = uuid } })
        }
        val movement3 = MovementModel().apply {
            content = listOf(MovementContentModel().apply { vehicle = VehicleModel().apply { id = UUID.randomUUID() } })
        }
        `when`(movementRepository.findAll(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(
            Flux.just(
                movement1,
                movement2,
                movement3
            )
        )

        // Act
        service.findVehicleMovements(
            eventId,
            uuid,
            order = ASC,
            onlyVisible = true,
            searched = null,
            type = null,
            startDateTime = null,
            endDateTime = null,
        ).blockFirst()

        // Assert
        verify(movementRepository, times(1)).findAll(
            eventId,
            onlyVisible = true,
            type = null,
            startDateTime = null,
            endDateTime = null,
        )
    }

    @Test
    fun `Should createVehicle create and return a Vehicle`() {
        // Arrange
        `when`(eventService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any())).thenReturn(Mono.just(eventId))
        `when`(repository.create(any())).thenReturn(Mono.just(vehicle0))

        // Act
        service.createVehicle(currentUser(), vehicle0).block()

        // Assert
        verify(eventService, times(1)).validateDateTimes(eventId, null, null, VEHICLE_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE)
    }

    @Test
    fun `Should updateVehicleById update and return a Vehicle`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(vehicle0))
        `when`(eventService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any())).thenReturn(Mono.just(eventId))
        `when`(repository.update(any())).thenReturn(Mono.just(vehicle1))

        // Act
        service.updateVehicleById(currentUser(), eventId, uuid, vehicle1).block()

        // Assert
        verify(eventService, times(1)).validateDateTimes(eventId, null, null, VEHICLE_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE)
        verify(repository, times(1)).update(vehicle1)
    }

    @Test
    fun `Should disableVehicleById hide and return a Vehicle`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(vehicle0))
        `when`(repository.update(any())).thenReturn(Mono.just(vehicle0))

        // Act
        service.disableVehicleById(currentUser(), eventId, uuid).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = true)
        verify(repository, times(1)).update(any())
    }

    @Test
    fun `Should enableVehicleById restore and return a Vehicle`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(vehicle0))
        `when`(repository.update(any())).thenReturn(Mono.just(vehicle0))

        // Act
        service.enableVehicleById(currentUser(), eventId, uuid).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = false)
        verify(repository, times(1)).update(any())
    }

    @Test
    fun `Should deleteVehicleById delete a Vehicle`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val vehicle = VehicleModel().apply {
            id = uuid; registration = "AB-123-CD"; brand = "Toyota"; model = "Hilux"; event =
            EventModel().apply { id = eventId }
        }
        `when`(repository.findById(any(), any(), any())).thenReturn(
            Mono.just(vehicle)
        )
        `when`(repository.deleteById(any())).thenReturn(Mono.empty())
        `when`(
            movementRepository.findAll(
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            )
        ).thenReturn(Flux.empty())

        // Act
        service.deleteVehicleById(currentUser(), eventId, uuid).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = false)
        verify(movementRepository, times(1)).findAll(
            eventId,
            onlyVisible = false,
            type = null,
            startDateTime = null,
            endDateTime = null
        )
        verify(repository, times(1)).deleteById(uuid)
    }

    @Test
    fun `Should deleteVehicleById throw RegistryException because Vehicle already has Movement`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val vehicle = VehicleModel().apply {
            id = uuid; registration = "AB-123-CD"; brand = "Toyota"; model = "Hilux"; event =
            EventModel().apply { id = eventId }
        }
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(vehicle))
        `when`(repository.deleteById(any())).thenReturn(Mono.empty())
        `when`(
            movementRepository.findAll(
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Flux.just(MovementModel().apply {
            content = listOf(MovementContentModel().apply { this.vehicle = VehicleModel().apply { id = uuid } })
        }))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.deleteVehicleById(currentUser(), eventId, uuid).block()
        }) as RegistryException

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = false)
        verify(movementRepository, times(1)).findAll(
            eventId,
            onlyVisible = false,
            type = null,
            startDateTime = null,
            endDateTime = null
        )
        verify(repository, times(0)).deleteById(any())

        assertEquals(FORBIDDEN, result.status)
        assertEquals(VEHICLE_DELETE_HAS_MOVEMENT, result.code)
    }
}
