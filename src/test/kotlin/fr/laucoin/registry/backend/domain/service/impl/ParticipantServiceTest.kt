package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_DELETE_HAS_MOVEMENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_GROUPS_NOT_FOUND_IN_PARTICIPANT_EVENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_GROUPS_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.repository.IGroupModelRepository
import fr.laucoin.registry.backend.domain.repository.IMovementModelRepository
import fr.laucoin.registry.backend.domain.repository.IParticipantModelRepository
import fr.laucoin.registry.backend.domain.service.IEventService
import fr.laucoin.registry.backend.domain.service.IParticipantService
import fr.laucoin.registry.backend.domain.service.IUserService
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
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.test.util.ReflectionTestUtils.setField
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class ParticipantServiceTest {
    private val repository: IParticipantModelRepository = mock()
    private val eventService: IEventService = mock()
    private val userService: IUserService = mock()
    private val movementRepository: IMovementModelRepository = mock()
    private val groupRepository: IGroupModelRepository = mock()
    private val service: IParticipantService =
        ParticipantService(repository, eventService, userService, movementRepository, groupRepository)

    companion object {
        private val participant0 = ParticipantModel().apply { lastName = "0"; event = EventModel().apply { id = eventId } }
        private val participant1 = ParticipantModel().apply { lastName = "1"; event = EventModel().apply { id = eventId } }
        private val participant2 = ParticipantModel().apply { lastName = "2"; event = EventModel().apply { id = eventId } }
        private val participant3 = ParticipantModel().apply { lastName = "3"; event = EventModel().apply { id = eventId } }

        private val participants = arrayOf(participant0, participant1, participant2, participant3)

        @JvmStatic
        fun `Should findParticipantsByEventId return Event's Participants`(): Stream<Arguments> = Stream.of(
            Arguments.of(ASC, null, participants.toList()),
            Arguments.of(DESC, null, participants.toList().reversed()),
            Arguments.of(ASC, "0", listOf(participant0)),
            Arguments.of(ASC, "1", listOf(participant1)),
            Arguments.of(ASC, "2", listOf(participant2)),
            Arguments.of(ASC, "3", listOf(participant3)),
            Arguments.of(DESC, "0", listOf(participant0)),
            Arguments.of(DESC, "1", listOf(participant1)),
            Arguments.of(DESC, "2", listOf(participant2)),
            Arguments.of(DESC, "3", listOf(participant3)),
            Arguments.of(ASC, "QWERTY", emptyList<ParticipantModel>()),
            Arguments.of(DESC, "QWERTY", emptyList<ParticipantModel>()),
        )

        @JvmStatic
        fun `Participant content`(): Stream<Arguments> = Stream.of(
            Arguments.of(ParticipantModel().apply { event = EventModel().apply { id = eventId } }, 0, 0),
            Arguments.of(
                ParticipantModel().apply {
                    user = UserModel().apply { id = UUID.randomUUID() }
                    event = EventModel().apply { id = eventId }
                },
                1,
                0,
            ),
            Arguments.of(
                ParticipantModel().apply {
                    groups = listOf(GroupModel().apply { id = UUID.randomUUID() })
                    event = EventModel().apply { id = eventId }
                },
                0,
                1,
            ),
            Arguments.of(
                ParticipantModel().apply {
                    user = UserModel().apply { id = UUID.randomUUID() }
                    groups = listOf(GroupModel().apply { id = UUID.randomUUID() })
                    event = EventModel().apply { id = eventId }
                },
                1,
                1,
            ),
        )

        @JvmStatic
        fun `Should createParticipant failed to valid Group`(): Stream<Arguments> = Stream.of(
            Arguments.of(
                Flux.empty<GroupModel>(),
                NOT_FOUND,
                PARTICIPANT_GROUPS_NOT_FOUND_IN_PARTICIPANT_EVENT,
            ),
            Arguments.of(
                Flux.just(GroupModel().apply { id = UUID.randomUUID(); visible = false }),
                CONFLICT,
                PARTICIPANT_GROUPS_NOT_VISIBLE,
            ),
        )
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findParticipantsByEventId return Event's Participants`(
        order: Direction,
        searched: String?,
        expectedList: List<ParticipantModel>,
    ) {
        // Arrange
        setField(service, "searchThreshold", 0.5)
        `when`(repository.findAll(any(), any(), any(), anyOrNull(), anyOrNull())).thenReturn(Flux.just(*participants))

        // Act
        val result = service.findParticipantsByEventId(
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
    fun `Should findParticipantById return the Participant`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(participant0))

        // Act
        service.findParticipantById(eventId, uuid, onlyVisible = true).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = true)
    }

    @ParameterizedTest
    @MethodSource("Participant content")
    fun `Should createParticipant create and return a Participant`(
        participant: ParticipantModel,
        expectedCallUserVerification: Int,
        expectedCallGroupVerification: Int,
    ) {
        // Arrange
        `when`(eventService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any())).thenReturn(Mono.just(eventId))
        `when`(repository.findAll(any(), any(), any(), anyOrNull(), anyOrNull())).thenReturn(Flux.empty())
        `when`(groupRepository.findAllByIds(any(), any(), any())).thenReturn(Flux.just(GroupModel()))
        `when`(repository.create(any())).thenReturn(Mono.just(participant))

        // Act
        service.createParticipant(currentUser(), participant).block()

        // Assert
        verify(eventService, times(1)).validateDateTimes(eventId, null, null, PARTICIPANT_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE)
        verify(repository, times(expectedCallUserVerification)).findAll(
            eventId,
            onlyVisible = false,
            onlyPresent = false,
            startDateTime = null,
            endDateTime = null
        )
        verify(groupRepository, times(expectedCallGroupVerification)).findAllByIds(
            eventId,
            ids = participant.groups.mapNotNull { g -> g.id },
            onlyVisible = false,
        )
        verify(repository, times(1)).create(participant)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should createParticipant failed to valid Group`(
        groups: Flux<GroupModel>,
        status: HttpStatus,
        errorMessage: String,
    ) {
        // Arrange
        val participant = ParticipantModel().apply {
            event = EventModel().apply { id = eventId }
            this.groups = listOf(GroupModel().apply { id = UUID.randomUUID() })
        }
        `when`(eventService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any())).thenReturn(Mono.just(eventId))
        `when`(groupRepository.findAllByIds(any(), any(), any())).thenReturn(groups)
        `when`(repository.create(any())).thenReturn(Mono.just(participant))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.createParticipant(currentUser(), participant).block()
        }) as RegistryException

        // Assert
        assertEquals(status, result.status)
        assertEquals(errorMessage, result.message)
        verify(eventService, times(1)).validateDateTimes(eventId, null, null, PARTICIPANT_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE)
        verify(groupRepository, times(1)).findAllByIds(
            eventId,
            ids = participant.groups.mapNotNull { g -> g.id },
            onlyVisible = false,
        )
    }

    @ParameterizedTest
    @MethodSource("Participant content")
    fun `Should updateParticipantById update and return a Participant`(
        participant: ParticipantModel,
        expectedCallUserVerification: Int,
        expectedCallGroupVerification: Int,
    ) {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(participant0))
        `when`(eventService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any())).thenReturn(Mono.just(eventId))
        `when`(repository.findAll(any(), any(), any(), anyOrNull(), anyOrNull())).thenReturn(Flux.empty())
        `when`(groupRepository.findAllByIds(any(), any(), any())).thenReturn(Flux.just(GroupModel()))
        `when`(repository.update(any())).thenReturn(Mono.just(participant))

        // Act
        service.updateParticipantById(currentUser(), eventId, uuid, participant).block()

        // Assert
        verify(eventService, times(1)).validateDateTimes(eventId, null, null, PARTICIPANT_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE)
        verify(repository, times(expectedCallUserVerification)).findAll(
            eventId,
            onlyVisible = false,
            onlyPresent = false,
            startDateTime = null,
            endDateTime = null
        )
        verify(groupRepository, times(expectedCallGroupVerification)).findAllByIds(
            eventId,
            ids = participant.groups.mapNotNull { g -> g.id },
            onlyVisible = false,
        )
        verify(repository, times(1)).update(participant)
    }

    @Test
    fun `Should disableParticipantById hide and return a Participant`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(participant0))
        `when`(repository.update(any())).thenReturn(Mono.just(participant0))

        // Act
        service.disableParticipantById(currentUser(), eventId, uuid).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = true)
        verify(repository, times(1)).update(any())
    }

    @Test
    fun `Should enableParticipantById restore and return a Participant`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(participant0))
        `when`(repository.update(any())).thenReturn(Mono.just(participant0))

        // Act
        service.enableParticipantById(currentUser(), eventId, uuid).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = false)
        verify(repository, times(1)).update(any())
    }

    @Test
    fun `Should deleteParticipantById delete a Participant`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(participant0.apply { id = uuid }))
        `when`(repository.deleteById(any())).thenReturn(Mono.empty())
        `when`(
            movementRepository.findAll(
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Flux.empty())

        // Act
        service.deleteParticipantById(currentUser(), eventId, uuid).block()

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
    fun `Should deleteParticipantById throw RegistryException because Participant already has Movement`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(participant0.apply { id = uuid }))
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
            content = listOf(MovementContentModel().apply { participant = ParticipantModel().apply { id = uuid } })
        }))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.deleteParticipantById(currentUser(), eventId, uuid).block()
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
        assertEquals(PARTICIPANT_DELETE_HAS_MOVEMENT, result.code)
    }
}
