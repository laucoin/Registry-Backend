package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventError.EVENT_DATE_CONFLICT_WITH_ELEMENTS
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.repository.IEventModelRepository
import fr.laucoin.registry.backend.domain.service.IEventService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserEventProfileService
import fr.laucoin.registry.backend.test.ModelExt.eventId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.lenient
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.never
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class EventServiceTest {
    private val repository: IEventModelRepository = mock()
    private val eventProfileService: IUserEventProfileService = mock()
    private val transactionalOperator: TransactionalOperator = mock()
    private val roleService: IRoleService = mock()
    private val service: IEventService = EventService(
        repository,
        eventProfileService,
        transactionalOperator,
        roleService
    )

    companion object {
        private val event0 = EventModel().apply { name = "0" }
        private val event1 = EventModel().apply { name = "1" }
        private val event2 = EventModel().apply { name = "2" }
        private val event3 = EventModel().apply { name = "3" }

        private val events = arrayOf(event0, event1, event2, event3)

        @JvmStatic
        fun `Should findEvents return Events`(): Stream<Arguments> = Stream.of(
            Arguments.of(ASC, null, events.toList()),
            Arguments.of(DESC, null, events.toList().reversed()),
            Arguments.of(ASC, "0", listOf(event0)),
            Arguments.of(ASC, "1", listOf(event1)),
            Arguments.of(ASC, "2", listOf(event2)),
            Arguments.of(ASC, "3", listOf(event3)),
            Arguments.of(DESC, "0", listOf(event0)),
            Arguments.of(DESC, "1", listOf(event1)),
            Arguments.of(DESC, "2", listOf(event2)),
            Arguments.of(DESC, "3", listOf(event3)),
            Arguments.of(ASC, "QWERTY", emptyList<EventModel>()),
            Arguments.of(DESC, "QWERTY", emptyList<EventModel>()),
        )

        @JvmStatic
        fun `Should validateDateTimes throw RegistryException for datetime out of range`(): Stream<Arguments> = Stream.of(
            Arguments.of(
                ZonedDateTime.of(1999, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")),
            ),
            Arguments.of(
                ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")),
            ),
            Arguments.of(
                ZonedDateTime.of(1999, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")),
            ),
        )

        @JvmStatic
        fun `Should updateEventById update and return an Event`(): Stream<Arguments> = Stream.of(
            Arguments.of(null, null, null, null, 0),
            Arguments.of(
                null, null,
                ZonedDateTime.of(2024, 9, 21, 0, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2024, 9, 25, 0, 0, 0, 0, ZoneId.of("UTC")),
                1,
            ),
            Arguments.of(
                ZonedDateTime.of(2024, 9, 21, 0, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2024, 9, 25, 0, 0, 0, 0, ZoneId.of("UTC")),
                null, null, 0,
            ),
            Arguments.of(
                ZonedDateTime.of(2024, 9, 22, 0, 0, 0, 0, ZoneId.of("UTC")),
                null,
                ZonedDateTime.of(2024, 9, 21, 0, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2024, 9, 25, 0, 0, 0, 0, ZoneId.of("UTC")),
                1,
            ),
            Arguments.of(
                ZonedDateTime.of(2024, 9, 21, 0, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2024, 9, 25, 0, 0, 0, 0, ZoneId.of("UTC")),
                null, null, 0,
            ),
            Arguments.of(
                ZonedDateTime.of(2024, 9, 22, 0, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2024, 9, 25, 0, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2024, 9, 21, 0, 0, 0, 0, ZoneId.of("UTC")),
                null, 0,
            ),
            Arguments.of(
                ZonedDateTime.of(2024, 9, 22, 0, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2024, 9, 26, 0, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2024, 9, 21, 0, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2024, 9, 27, 0, 0, 0, 0, ZoneId.of("UTC")),
                0,
            ),
            Arguments.of(
                ZonedDateTime.of(2024, 9, 22, 0, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2024, 9, 26, 0, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2024, 9, 21, 0, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2024, 9, 25, 0, 0, 0, 0, ZoneId.of("UTC")),
                1,
            ),
            Arguments.of(
                ZonedDateTime.of(2024, 9, 22, 0, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2024, 9, 26, 0, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2024, 9, 23, 0, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2024, 9, 27, 0, 0, 0, 0, ZoneId.of("UTC")),
                1,
            ),
            Arguments.of(
                ZonedDateTime.of(2024, 9, 22, 0, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2024, 9, 26, 0, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2024, 9, 23, 0, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2024, 9, 25, 0, 0, 0, 0, ZoneId.of("UTC")),
                1,
            ),
        )

        @JvmStatic
        fun `Should updateEventById throw RegistryException`(): Stream<Arguments> = Stream.of(
            Arguments.of(
                ZonedDateTime.of(2024, 9, 24, 0, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2024, 9, 25, 0, 0, 0, 0, ZoneId.of("UTC")),
            ),
            Arguments.of(
                ZonedDateTime.of(2024, 9, 21, 0, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2024, 9, 22, 0, 0, 0, 0, ZoneId.of("UTC")),
            ),
            Arguments.of(
                ZonedDateTime.of(2024, 9, 24, 0, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2024, 9, 22, 0, 0, 0, 0, ZoneId.of("UTC")),
            ),
        )
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findEvents return Events`(
        order: Direction,
        searched: String?,
        expectedList: List<EventModel>,
    ) {
        // Arrange
        `when`(repository.findAll(any(), anyOrNull(), anyOrNull())).thenReturn(Flux.just(*events))
        `when`(roleService.getAuthoritiesByUserRole(anyOrNull())).thenReturn(listOf("REGISTRY_EVENT_R"))

        // Act
        val result = service.findEvents(
            currentUser(),
            order,
            onlyVisible = true,
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
    fun `Should findEventById return the Event`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any())).thenReturn(Mono.just(event0))

        // Act
        service.findEventById(uuid, onlyVisible = true).block()

        // Assert
        verify(repository, times(1)).findById(uuid, onlyVisible = true)
    }

    @Test
    fun `Should validateDateTime throw RegistryException for datetime out of range`() {
        // Arrange
        val message = "MESSAGE"
        val now = ZonedDateTime.now()
        val event = EventModel().apply {
            id = eventId
            begin = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
            end = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
        }
        `when`(repository.findById(any(), any())).thenReturn(Mono.just(event))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.validateDateTime(eventId, now, message).block()
        }) as RegistryException

        // Assert
        assertEquals(CONFLICT, result.status)
        assertEquals(message, result.message)
        assertEquals(3, result.args?.size)
        assertEquals(now.toString(), result.args?.first())
        assertEquals(event.begin.toString(), result.args?.get(1))
        assertEquals(event.end.toString(), result.args?.get(2))

        verify(repository, times(1)).findById(eventId, onlyVisible = false)
    }

    @Test
    fun `Should validateDateTime not throw`() {
        // Arrange
        val message = "MESSAGE"
        val now = ZonedDateTime.now()
        val event = EventModel().apply {
            id = eventId
            begin = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
            end = ZonedDateTime.of(3000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
        }
        `when`(repository.findById(any(), any())).thenReturn(Mono.just(event))

        // Act
        val result = service.validateDateTime(eventId, now, message).block()

        // Assert
        assertEquals(eventId, result)

        verify(repository, times(1)).findById(eventId, onlyVisible = false)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should validateDateTimes throw RegistryException for datetime out of range`(
        newStart: ZonedDateTime,
        newEnd: ZonedDateTime,
    ) {
        // Arrange
        val message = "MESSAGE"
        val event = EventModel().apply {
            id = eventId
            begin = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
            end = ZonedDateTime.of(2010, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
        }
        `when`(repository.findById(any(), any())).thenReturn(Mono.just(event))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.validateDateTimes(eventId, newStart, newEnd, message).block()
        }) as RegistryException

        // Assert
        assertEquals(CONFLICT, result.status)
        assertEquals(message, result.message)
        assertEquals(4, result.args?.size)
        assertEquals(newStart.toString(), result.args?.first())
        assertEquals(newEnd.toString(), result.args?.get(1))
        assertEquals(event.begin.toString(), result.args?.get(2))
        assertEquals(event.end.toString(), result.args?.get(3))

        verify(repository, times(1)).findById(eventId, onlyVisible = false)
    }

    @Test
    fun `Should validateDateTimes not throw`() {
        // Arrange
        val message = "MESSAGE"
        val newStart: ZonedDateTime = ZonedDateTime.now()
        val newEnd: ZonedDateTime = ZonedDateTime.now()
        val event = EventModel().apply {
            id = eventId
            begin = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
            end = ZonedDateTime.of(3000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
        }
        `when`(repository.findById(any(), any())).thenReturn(Mono.just(event))

        // Act
        val result = service.validateDateTimes(eventId, newStart, newEnd, message).block()

        // Assert
        assertEquals(eventId, result)

        verify(repository, times(1)).findById(eventId, onlyVisible = false)
    }

    @Test
    fun `Should createEvent create and return a Event`() {
        // Arrange
        val currentUser = currentUser()
        `when`(repository.create(any())).thenReturn(Mono.just(event0))
        `when`(transactionalOperator.transactional(any<Mono<*>>())).thenReturn(Mono.just(event0))

        // Act
        service.createEvent(currentUser, event0).block()

        // Assert
        verify(repository, times(1)).create(event0)
        verify(transactionalOperator, times(1)).transactional(any<Mono<*>>())
    }

    @ParameterizedTest
    @MethodSource
    fun `Should updateEventById update and return an Event`(
        oldBegin: ZonedDateTime?,
        oldFinish: ZonedDateTime?,
        newBegin: ZonedDateTime?,
        newFinish: ZonedDateTime?,
        expectedFindMovementsCall: Int,
    ) {
        val uuid = UUID.randomUUID()
        val currentUser = currentUser()
        val oldEvent = EventModel().apply {
            id = eventId
            begin = oldBegin
            end = oldFinish
        }
        val newEvent = EventModel().apply {
            id = eventId
            begin = newBegin
            end = newFinish
        }
        `when`(repository.findById(any(), any())).thenReturn(Mono.just(oldEvent))
        lenient().`when`(repository.validDateTime(any(), anyOrNull(), anyOrNull()))
            .thenReturn(Mono.just(true))
        `when`(repository.update(any())).thenReturn(Mono.just(newEvent))

        // Act
        service.updateEventById(currentUser, uuid, newEvent).block()

        // Assert
        verify(repository, times(1)).findById(uuid, onlyVisible = false)
        verify(repository, times(expectedFindMovementsCall)).validDateTime(
            id = eventId,
            begin = newBegin,
            end = newFinish,
        )
        verify(repository, times(1)).update(newEvent)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should updateEventById throw RegistryException`(
        newBegin: ZonedDateTime?,
        newFinish: ZonedDateTime?,
    ) {
        val uuid = UUID.randomUUID()
        val currentUser = currentUser()
        val oldEvent = EventModel().apply {
            id = eventId
            begin = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
            end = ZonedDateTime.of(3000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
        }
        val newEvent = EventModel().apply {
            id = eventId
            begin = newBegin
            end = newFinish
        }
        `when`(repository.findById(any(), any())).thenReturn(Mono.just(oldEvent))
        lenient().`when`(repository.validDateTime(any(), anyOrNull(), anyOrNull()))
            .thenReturn(Mono.just(false))
        `when`(repository.update(any())).thenReturn(Mono.just(newEvent))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.updateEventById(currentUser, uuid, newEvent).block()
        }) as RegistryException

        // Assert
        assertEquals(CONFLICT, result.status)
        assertEquals(EVENT_DATE_CONFLICT_WITH_ELEMENTS, result.message)

        verify(repository, times(1)).findById(uuid, onlyVisible = false)
        verify(repository, times(1)).validDateTime(
            id = eventId,
            begin = newBegin,
            end = newFinish,
        )
        verify(repository, never()).update(any())
    }

    @Test
    fun `Should disableEventById hide and return an Event`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any())).thenReturn(Mono.just(event0))
        `when`(repository.update(any())).thenReturn(Mono.just(event0))

        // Act
        service.disableEventById(currentUser(), uuid).block()

        // Assert
        verify(repository, times(1)).findById(uuid, onlyVisible = true)
        verify(repository, times(1)).update(any())
    }

    @Test
    fun `Should enableEventById restore and return an Event`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any())).thenReturn(Mono.just(event0))
        `when`(repository.update(any())).thenReturn(Mono.just(event0))

        // Act
        service.enableEventById(currentUser(), uuid).block()

        // Assert
        verify(repository, times(1)).findById(uuid, onlyVisible = false)
        verify(repository, times(1)).update(any())
    }

    @Test
    fun `Should deleteEventById delete a Participant`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any())).thenReturn(Mono.just(event0))
        `when`(repository.deleteById(any())).thenReturn(Mono.empty())

        // Act
        service.deleteEventById(uuid).block()

        // Assert
        verify(repository, times(1)).findById(uuid, onlyVisible = false)
        verify(repository, times(1)).deleteById(uuid)
    }
}
