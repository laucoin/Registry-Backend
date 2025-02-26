package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_LAST_MEMBERS_CANNOT_BE_REMOVED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_ALREADY_ADDED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_NOT_FOUND_IN_GROUP_EVENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.repository.IGroupModelRepository
import fr.laucoin.registry.backend.domain.repository.IParticipantModelRepository
import fr.laucoin.registry.backend.domain.service.IEventService
import fr.laucoin.registry.backend.domain.service.IGroupService
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
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class GroupServiceTest {
    private val repository: IGroupModelRepository = mock()
    private val participantRepository: IParticipantModelRepository = mock()
    private val eventService: IEventService = mock()
    private val service: IGroupService = GroupService(repository, participantRepository, eventService)

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
        private val group0 = GroupModel().apply { name = "group0"; event = event0 }
        private val group1 = GroupModel().apply { name = "group1"; event = event1 }
        private val group2 = GroupModel().apply { name = "group2"; event = event2 }
        private val group3 = GroupModel().apply { name = "group3"; event = event3 }

        private val groups = arrayOf(group0, group1, group2, group3)

        @JvmStatic
        fun `Should findGroups return Event's Groups`(): Stream<Arguments> = Stream.of(
            Arguments.of(ASC, null, groups.toList()),
            Arguments.of(DESC, null, groups.toList().reversed()),
            Arguments.of(ASC, "0", listOf(group0)),
            Arguments.of(ASC, "1", listOf(group1)),
            Arguments.of(ASC, "2", listOf(group2)),
            Arguments.of(ASC, "3", listOf(group3)),
            Arguments.of(DESC, "0", listOf(group0)),
            Arguments.of(DESC, "1", listOf(group1)),
            Arguments.of(DESC, "2", listOf(group2)),
            Arguments.of(DESC, "3", listOf(group3)),
            Arguments.of(ASC, "QWERTY", emptyList<GroupModel>()),
            Arguments.of(DESC, "QWERTY", emptyList<GroupModel>()),
        )

        @JvmStatic
        fun `Should updateGroupById update and return a Group`(): Stream<Arguments> = Stream.of(
            Arguments.of(
                GroupModel().apply {
                    members = emptyList()
                    event = EventModel().apply { id = eventId }
                },
                0,
            ),
            Arguments.of(
                GroupModel().apply {
                    members = listOf(ParticipantModel().apply { id = UUID.randomUUID() })
                    event = EventModel().apply { id = eventId }
                },
                1,
            )
        )

        @JvmStatic
        fun `Should updateGroupById failed to valid Participant`(): Stream<Arguments> = Stream.of(
            Arguments.of(
                Flux.empty<GroupModel>(),
                NOT_FOUND,
                GROUP_MEMBERS_NOT_FOUND_IN_GROUP_EVENT,
            ),
            Arguments.of(
                Flux.just(ParticipantModel().apply { id = UUID.randomUUID(); visible = false }),
                CONFLICT,
                GROUP_MEMBERS_NOT_VISIBLE,
            ),
        )
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findGroups return Event's Groups`(
        order: Direction,
        searched: String?,
        expectedList: List<GroupModel>,
    ) {
        // Arrange
        `when`(repository.findAll(any(), any(), any(), anyOrNull(), anyOrNull())).thenReturn(Flux.just(*groups))

        // Act
        val result = service.findGroups(
            eventId,
            order,
            onlyVisible = true,
            onlyPresent = false,
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
    fun `Should findGroupMembersByGroupId return the Group`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val expectedParticipant = ParticipantModel().apply { groups = listOf(GroupModel().apply { id = uuid }) }
        `when`(participantRepository.findAll(any(), any(), any(), anyOrNull(), anyOrNull())).thenReturn(
            Flux.just(expectedParticipant, ParticipantModel())
        )

        // Act
        val result = service.findGroupMembersByGroupId(
            eventId,
            uuid,
            order = ASC,
            onlyVisible = true,
            onlyPresent = false,
            searched = null,
            startDateTime = null,
            endDateTime = null,
        ).collectList().block()

        // Assert
        assertEquals(1, result?.size)
        assertEquals(expectedParticipant, result?.first())
        verify(participantRepository, times(1)).findAll(
            eventId,
            onlyVisible = true,
            onlyPresent = false,
            startDateTime = null,
            endDateTime = null,
        )
    }

    @Test
    fun `Should findGroupById return the Group`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(group0))

        // Act
        service.findGroupById(eventId, uuid, onlyVisible = true).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = true)
    }

    @Test
    fun `Should searchParticipants return the searched Participant`() {
        // Arrange
        val searched = "John"
        `when`(
            participantRepository.findAll(
                any(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
            )
        ).thenReturn(Flux.empty())

        // Act
        service.searchParticipants(eventId, searched).collectList().block()

        // Assert
        verify(participantRepository, times(1)).findAll(
            eventId,
            onlyVisible = true,
            onlyPresent = false,
            startDateTime = null,
            endDateTime = null,
        )
    }

    @Test
    fun `Should createGroup create and return a Group`() {
        // Arrange
        val participantId: UUID = UUID.randomUUID()
        val group = GroupModel().apply {
            members = listOf(ParticipantModel().apply { id = participantId })
            event = event0
        }
        `when`(eventService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any())).thenReturn(Mono.just(eventId))
        `when`(participantRepository.findAllByIds(any(), any(), any())).thenReturn(Flux.just(ParticipantModel()))
        `when`(repository.create(any())).thenReturn(Mono.just(group))

        // Act
        service.createGroup(currentUser(), group).block()

        // Assert
        verify(eventService, times(1)).validateDateTimes(eventId, null, null, GROUP_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE)
        verify(participantRepository, times(1)).findAllByIds(eventId, listOf(participantId), onlyVisible = false)
        verify(repository, times(1)).create(any())
    }

    @ParameterizedTest
    @MethodSource
    fun `Should updateGroupById update and return a Group`(
        group: GroupModel,
        expectedCallParticipantVerification: Int,
    ) {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(eventService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any())).thenReturn(Mono.just(eventId))
        `when`(participantRepository.findAllByIds(any(), any(), any())).thenReturn(Flux.just(ParticipantModel()))
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(GroupModel()))
        `when`(repository.update(any())).thenReturn(Mono.just(group))

        // Act
        service.updateGroupById(currentUser(), eventId, uuid, group).block()

        // Assert
        verify(eventService, times(1)).validateDateTimes(eventId, null, null, GROUP_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE)
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
    fun `Should updateGroupById failed to valid Participant`(
        participants: Flux<ParticipantModel>,
        status: HttpStatus,
        errorMessage: String,
    ) {
        // Arrange
        val group: GroupModel = GroupModel().apply {
            event = EventModel().apply { id = eventId }
            members =
                listOf(ParticipantModel().apply { id = UUID.randomUUID() })
        }
        val uuid = UUID.randomUUID()
        `when`(eventService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any())).thenReturn(Mono.just(eventId))
        `when`(participantRepository.findAllByIds(any(), any(), any())).thenReturn(participants)
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(GroupModel()))
        `when`(repository.update(any())).thenReturn(Mono.just(group))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.updateGroupById(currentUser(), eventId, uuid, group).block()
        }) as RegistryException

        // Assert
        assertEquals(status, result.status)
        assertEquals(errorMessage, result.message)
        verify(eventService, times(1)).validateDateTimes(eventId, null, null, GROUP_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE)
        verify(participantRepository, times(1)).findAllByIds(
            eventId,
            ids = group.members.mapNotNull { p -> p.id },
            onlyVisible = false,
        )
    }

    @Test
    fun `Should addMembersToGroupById return the list of added Participant id`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val memberIds: List<UUID> = listOf(uuid)
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(GroupModel()))
        `when`(participantRepository.findAllByIds(any(), any(), any())).thenReturn(Flux.just(ParticipantModel()))
        `when`(repository.update(any())).thenReturn(Mono.just(GroupModel()))

        // Act
        service.addMembersToGroupById(currentUser(), eventId, uuid, memberIds).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = false)
        verify(participantRepository, times(1)).findAllByIds(
            eq(eventId),
            any(),
            onlyVisible = eq(false)
        )
        verify(repository, times(1)).update(any())
    }

    @Test
    fun `Should addMembersToGroupById throw if attempt to add an already added list of Participant id`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val memberIds: List<UUID> = listOf(uuid)
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(GroupModel().apply {
            members = listOf(ParticipantModel().apply { id = uuid })
        }))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.addMembersToGroupById(currentUser(), eventId, uuid, memberIds).block()
        }) as RegistryException

        // Assert
        assertEquals(CONFLICT, result.status)
        assertEquals(GROUP_MEMBERS_ALREADY_ADDED, result.message)
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = false)
    }

    @Test
    fun `Should removeMemberFromGroupById return the updated Group`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(GroupModel().apply {
            members = listOf(ParticipantModel().apply { id = uuid })
        }))
        `when`(repository.update(any())).thenReturn(Mono.just(GroupModel()))

        // Act
        service.removeMemberFromGroupById(currentUser(), eventId, uuid, UUID.randomUUID()).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = false)
        verify(repository, times(1)).update(any())
    }

    @Test
    fun `Should removeMemberFromGroupById throw if attempt to remove the last Group Member`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(GroupModel().apply {
            members = listOf(ParticipantModel().apply { id = uuid })
        }))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.removeMemberFromGroupById(currentUser(), eventId, uuid, uuid).block()
        }) as RegistryException

        // Assert
        assertEquals(FORBIDDEN, result.status)
        assertEquals(GROUP_LAST_MEMBERS_CANNOT_BE_REMOVED, result.message)
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = false)
    }

    @Test
    fun `Should disableGroupById hide and return a Group`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(group0))
        `when`(repository.update(any())).thenReturn(Mono.just(group0))

        // Act
        service.disableGroupById(currentUser(), eventId, uuid).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = true)
        verify(repository, times(1)).update(any())
    }

    @Test
    fun `Should enableGroupById restore and return a Group`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(group0))
        `when`(repository.update(any())).thenReturn(Mono.just(group0))

        // Act
        service.enableGroupById(currentUser(), eventId, uuid).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = false)
        verify(repository, times(1)).update(any())
    }

    @Test
    fun `Should deleteGroupById delete a Group`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(group0.apply { id = uuid }))
        `when`(repository.deleteById(any())).thenReturn(Mono.empty())

        // Act
        service.deleteGroupById(eventId, uuid).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = false)
        verify(repository, times(1)).deleteById(uuid)
    }
}
