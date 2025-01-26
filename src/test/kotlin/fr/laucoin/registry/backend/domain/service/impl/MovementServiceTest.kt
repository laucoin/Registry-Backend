package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DATETIME_OUT_OF_EVENT_DATE_RANGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_EVENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANTS_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.repository.IGroupModelRepository
import fr.laucoin.registry.backend.domain.repository.IMovementModelRepository
import fr.laucoin.registry.backend.domain.repository.IParticipantModelRepository
import fr.laucoin.registry.backend.domain.service.IEventService
import fr.laucoin.registry.backend.domain.service.IMovementService
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
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.test.util.ReflectionTestUtils.setField
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class MovementServiceTest {
    private val repository: IMovementModelRepository = mock()
    private val eventService: IEventService = mock()
    private val participantRepository: IParticipantModelRepository = mock()
    private val groupRepository: IGroupModelRepository = mock()
    private val service: IMovementService = MovementService(repository, eventService, participantRepository, groupRepository)

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

        @JvmStatic
        fun `Should updateMovementById update and return a Movement`(): Stream<Arguments> = Stream.of(
            Arguments.of(
                MovementModel().apply {
                    content = emptyList()
                    event = EventModel().apply { id = eventId }
                },
                0,
            ),
            Arguments.of(
                MovementModel().apply {
                    content =
                        listOf(MovementContentModel().apply { participant = ParticipantModel().apply { id = UUID.randomUUID() } })
                    event = EventModel().apply { id = eventId }
                },
                1,
            )
        )

        @JvmStatic
        fun `Should updateMovementById failed to valid Participant`(): Stream<Arguments> = Stream.of(
            Arguments.of(
                Flux.empty<GroupModel>(),
                NOT_FOUND,
                MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_EVENT,
            ),
            Arguments.of(
                Flux.just(ParticipantModel().apply { id = UUID.randomUUID(); visible = false }),
                CONFLICT,
                MOVEMENT_PARTICIPANTS_NOT_VISIBLE,
            ),
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
        val participantId: UUID = UUID.randomUUID()
        val movement = MovementModel().apply {
            dateTime = ZonedDateTime.now()
            content = listOf(MovementContentModel().apply { participant = ParticipantModel().apply { id = participantId } })
            event = event0
        }
        `when`(eventService.validateDateTime(any(), anyOrNull(), any())).thenReturn(Mono.just(eventId))
        `when`(participantRepository.findAllByIds(any(), any(), any())).thenReturn(Flux.just(ParticipantModel()))
        `when`(repository.create(any())).thenReturn(Mono.just(movement0))

        // Act
        service.createMovement(currentUser(), movement).block()

        // Assert
        verify(eventService, times(1)).validateDateTime(eventId, movement.dateTime, MOVEMENT_DATETIME_OUT_OF_EVENT_DATE_RANGE)
        verify(participantRepository, times(1)).findAllByIds(eventId, listOf(participantId), onlyVisible = false)
        verify(repository, times(1)).create(any())
    }

    @ParameterizedTest
    @MethodSource
    fun `Should updateMovementById update and return a Movement`(
        movement: MovementModel,
        expectedCallParticipantVerification: Int,
    ) {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(eventService.validateDateTime(any(), anyOrNull(), any())).thenReturn(Mono.just(eventId))
        `when`(participantRepository.findAllByIds(any(), any(), any())).thenReturn(Flux.just(ParticipantModel()))
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(MovementModel()))
        `when`(repository.update(any())).thenReturn(Mono.just(movement))

        // Act
        service.updateMovementById(currentUser(), eventId, uuid, movement).block()

        // Assert
        verify(eventService, times(1)).validateDateTime(eventId, movement.dateTime, MOVEMENT_DATETIME_OUT_OF_EVENT_DATE_RANGE)
        verify(participantRepository, times(expectedCallParticipantVerification)).findAllByIds(
            eq(eventId),
            any(),
            onlyVisible = eq(false)
        )
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = false)
        verify(repository, times(1)).update(any())
    }

    @ParameterizedTest
    @MethodSource
    fun `Should updateMovementById failed to valid Participant`(
        participants: Flux<ParticipantModel>,
        status: HttpStatus,
        errorMessage: String,
    ) {
        // Arrange
        val movement: MovementModel = MovementModel().apply {
            dateTime = ZonedDateTime.now()
            event = EventModel().apply { id = eventId }
            content =
                listOf(MovementContentModel().apply { participant = ParticipantModel().apply { id = UUID.randomUUID() } })
        }
        val uuid = UUID.randomUUID()
        `when`(eventService.validateDateTime(any(), anyOrNull(), any())).thenReturn(Mono.just(eventId))
        `when`(participantRepository.findAllByIds(any(), any(), any())).thenReturn(participants)
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(MovementModel()))
        `when`(repository.update(any())).thenReturn(Mono.just(movement))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.updateMovementById(currentUser(), eventId, uuid, movement).block()
        }) as RegistryException

        // Assert
        assertEquals(status, result.status)
        assertEquals(errorMessage, result.message)
        verify(eventService, times(1)).validateDateTime(eventId, movement.dateTime, MOVEMENT_DATETIME_OUT_OF_EVENT_DATE_RANGE)
        verify(participantRepository, times(1)).findAllByIds(
            eventId,
            ids = movement.content.mapNotNull { c -> c.participant?.id },
            onlyVisible = false,
        )
    }

    @Test
    fun `Should disableMovementById hide and return a Movement`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(movement0))
        `when`(repository.update(any())).thenReturn(Mono.just(movement0))

        // Act
        service.disableMovementById(currentUser(), eventId, uuid).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = true)
        verify(repository, times(1)).update(any())
    }

    @Test
    fun `Should enableMovementById restore and return a Movement`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(movement0))
        `when`(repository.update(any())).thenReturn(Mono.just(movement0))

        // Act
        service.enableMovementById(currentUser(), eventId, uuid).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = false)
        verify(repository, times(1)).update(any())
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
