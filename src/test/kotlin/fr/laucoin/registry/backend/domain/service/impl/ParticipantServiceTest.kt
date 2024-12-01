package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.repository.IParticipantModelRepository
import fr.laucoin.registry.backend.domain.service.IEventService
import fr.laucoin.registry.backend.domain.service.IParticipantService
import fr.laucoin.registry.backend.test.ModelExt.eventId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.time.LocalDate
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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class ParticipantServiceTest {
    private val repository: IParticipantModelRepository = mock()
    private val eventService: IEventService = mock()
    private val service: IParticipantService = ParticipantService(repository, eventService)

    companion object {
        private val participant0 = ParticipantModel().apply { lastName = "0" }
        private val participant1 = ParticipantModel().apply { lastName = "1" }
        private val participant2 = ParticipantModel().apply { lastName = "2" }
        private val participant3 = ParticipantModel().apply { lastName = "3" }

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
        `when`(repository.findAll(any(), any(), anyOrNull(), anyOrNull())).thenReturn(Flux.just(*participants))

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

    @Test
    fun `Should createParticipant create and return a Participant`() {
        // Arrange
        val participant = ParticipantModel().apply { event = EventModel().apply { id = eventId } }
        `when`(eventService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any())).thenReturn(Mono.just(eventId))
        `when`(repository.save(any())).thenReturn(Mono.just(participant))

        // Act
        service.createParticipant(currentUser(), participant).block()

        // Assert
        verify(repository, times(1)).save(participant)
    }

    @Test
    fun `Should updateParticipantById update and return a Participant`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val updated = ParticipantModel().apply {
            firstName = "Jane"
            lastName = "MILLER"
            birthday = LocalDate.of(2000, 1, 1)
            event = EventModel().apply { id = eventId }
        }
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(participant0))
        `when`(repository.save(any())).thenReturn(Mono.just(participant0))
        `when`(eventService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any())).thenReturn(Mono.just(eventId))

        // Act
        service.updateParticipantById(currentUser(), eventId, uuid, updated).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = false)
        verify(repository, times(1)).save(any())
    }

    @Test
    fun `Should disableParticipantById hide and return a Participant`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(participant0))
        `when`(repository.save(any())).thenReturn(Mono.just(participant0))

        // Act
        service.disableParticipantById(currentUser(), eventId, uuid).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = true)
        verify(repository, times(1)).save(any())
    }

    @Test
    fun `Should enableParticipantById restore and return a Participant`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(participant0))
        `when`(repository.save(any())).thenReturn(Mono.just(participant0))

        // Act
        service.enableParticipantById(currentUser(), eventId, uuid).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = false)
        verify(repository, times(1)).save(any())
    }

    @Test
    fun `Should deleteParticipantById delete a Participant`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(participant0.apply { id = uuid }))
        `when`(repository.deleteById(any())).thenReturn(Mono.empty())

        // Act
        service.deleteParticipantById(currentUser(), eventId, uuid).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = false)
        verify(repository, times(1)).deleteById(uuid)
    }
}
