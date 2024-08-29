package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DATETIME_OUT_OF_EVENT_DATE_RANGE
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.repository.IMovementContentModelRepository
import fr.laucoin.registry.backend.domain.repository.IMovementModelRepository
import fr.laucoin.registry.backend.domain.service.IEventService
import fr.laucoin.registry.backend.domain.service.IMovementService
import fr.laucoin.registry.backend.test.ModelExt.eventId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
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
import org.springframework.test.util.ReflectionTestUtils.setField
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class MovementServiceTest {
    private val repository: IMovementModelRepository = mock()
    private val contentRepository: IMovementContentModelRepository = mock()
    private val eventService: IEventService = mock()
    private val transactionalOperator: TransactionalOperator = mock()
    private val service: IMovementService = MovementService(repository, contentRepository, transactionalOperator, eventService)

    companion object {
        private val event0 = EventModel().apply {
            id = eventId
            name = "0"
            begin = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
            end = ZonedDateTime.of(3000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
        }
        private val event1 = EventModel().apply {
            id = eventId
            name = "1"
            begin = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
            end = ZonedDateTime.of(3000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
        }
        private val event2 = EventModel().apply {
            id = eventId
            name = "2"
            begin = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
            end = ZonedDateTime.of(3000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
        }
        private val event3 = EventModel().apply {
            id = eventId
            name = "3"
            begin = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
            end = ZonedDateTime.of(3000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
        }
        private val movement0 = MovementModel().apply { dateTime = ZonedDateTime.now(); event = event0 }
        private val movement1 = MovementModel().apply { dateTime = ZonedDateTime.now(); event = event1 }
        private val movement2 = MovementModel().apply { dateTime = ZonedDateTime.now(); event = event2 }
        private val movement3 = MovementModel().apply { dateTime = ZonedDateTime.now(); event = event3 }

        private val movements = arrayOf(movement0, movement1, movement2, movement3)

        @JvmStatic
        fun `Should findMovements return Event's Movements`(): Stream<Arguments> = Stream.of(
            Arguments.of(ASC, null, movements.toList()),
            Arguments.of(DESC, null, movements.toList().reversed()),
            Arguments.of(ASC, "0", listOf(movement0)),
            Arguments.of(ASC, "1", listOf(movement1)),
            Arguments.of(ASC, "2", listOf(movement2)),
            Arguments.of(ASC, "3", listOf(movement3)),
            Arguments.of(DESC, "0", listOf(movement0)),
            Arguments.of(DESC, "1", listOf(movement1)),
            Arguments.of(DESC, "2", listOf(movement2)),
            Arguments.of(DESC, "3", listOf(movement3)),
            Arguments.of(ASC, "QWERTY", emptyList<MovementModel>()),
            Arguments.of(DESC, "QWERTY", emptyList<MovementModel>()),
        )
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findMovements return Event's Movements`(
        order: Direction,
        searched: String?,
        expectedList: List<MovementModel>,
    ) {
        // Arrange
        setField(service, "searchThreshold", 0.5)
        `when`(repository.findAll(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Flux.just(*movements))

        // Act
        val result = service.findMovements(
            eventId,
            order,
            onlyVisible = true,
            searched,
            type = null,
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
    fun `Should findMovementById return the Movement`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(movement0))

        // Act
        service.findMovementById(eventId, uuid, onlyVisible = true).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = true)
    }

    @Test
    fun `Should createMovement create and return a Movement`() {
        // Arrange
        val currentUser = currentUser()
        `when`(eventService.validateDateTime(any(), any(), any())).thenReturn(Mono.just(eventId))
        `when`(transactionalOperator.transactional(any<Mono<*>>())).thenReturn(Mono.just(movement0))

        // Act
        service.createMovement(currentUser, movement0).block()

        // Assert
        verify(eventService, times(1)).validateDateTime(eventId, movement0.dateTime, MOVEMENT_DATETIME_OUT_OF_EVENT_DATE_RANGE)
        verify(transactionalOperator, times(1)).transactional(any<Mono<*>>())
    }

    @Test
    fun `Should updateMovementById update and return an Movement`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val currentUser = currentUser()
        `when`(eventService.validateDateTime(any(), any(), any())).thenReturn(Mono.just(eventId))
        `when`(transactionalOperator.transactional(any<Mono<*>>())).thenReturn(Mono.just(movement0))

        // Act
        service.updateMovementById(currentUser, eventId, uuid, movement0).block()

        // Assert
        verify(eventService, times(1)).validateDateTime(eventId, movement0.dateTime, MOVEMENT_DATETIME_OUT_OF_EVENT_DATE_RANGE)
        verify(transactionalOperator, times(1)).transactional(any<Mono<*>>())
    }

    @Test
    fun `Should disableMovementById hide and return a Movement`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(movement0))
        `when`(repository.save(any())).thenReturn(Mono.just(movement0))

        // Act
        service.disableMovementById(currentUser(), eventId, uuid).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = true)
        verify(repository, times(1)).save(any())
    }

    @Test
    fun `Should enableMovementById restore and return a Movement`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(movement0))
        `when`(repository.save(any())).thenReturn(Mono.just(movement0))

        // Act
        service.enableMovementById(currentUser(), eventId, uuid).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = false)
        verify(repository, times(1)).save(any())
    }

    @Test
    fun `Should deleteMovementById delete a Movement`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(movement0.apply { id = uuid }))
        `when`(repository.deleteById(any())).thenReturn(Mono.empty())

        // Act
        service.deleteMovementById(eventId, uuid).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = false)
        verify(repository, times(1)).deleteById(uuid)
    }
}
