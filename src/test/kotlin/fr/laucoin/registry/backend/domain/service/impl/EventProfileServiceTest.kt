package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_ALREADY_EXIST_ON_RANGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_ASSIGNS_ROLE_HIGHER_THAN_CURRENT_USER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_UPDATE_ROLE_HIGHER_THAN_CURRENT_USER
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.REJECTED
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.repository.IEventProfileModelRepository
import fr.laucoin.registry.backend.domain.service.IEventService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserEventProfileService
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class EventProfileServiceTest {
    private val repository: IEventProfileModelRepository = mock()
    private val profileService: IUserEventProfileService = mock()
    private val eventService: IEventService = mock()
    private val roleService: IRoleService = mock()
    private val userService: IUserService = mock()
    private val service = EventProfileService(repository, profileService, eventService, roleService, userService)

    companion object {
        private const val OTHER_EVENT_ROLE = "OTHER_EVENT_ROLE"
        private const val EVENT_ROLE = "EVENT_ROLE"
        private val profile0 = EventProfileModel().apply { role = EVENT_ROLE; event = EventModel().apply { name = "0" } }
        private val profile1 = EventProfileModel().apply { role = EVENT_ROLE; event = EventModel().apply { name = "1" } }
        private val profile2 = EventProfileModel().apply { role = EVENT_ROLE; event = EventModel().apply { name = "2" } }
        private val profile3 = EventProfileModel().apply { role = EVENT_ROLE; event = EventModel().apply { name = "3" } }

        private val profiles = arrayOf(profile0, profile1, profile2, profile3)

        @JvmStatic
        fun `Should findEventProfilesByEventId return Event's Profile`(): Stream<Arguments> = Stream.of(
            Arguments.of(ASC, null, profiles.toList()),
            Arguments.of(DESC, null, profiles.toList().reversed()),
            Arguments.of(ASC, "0", listOf(profile0)),
            Arguments.of(ASC, "1", listOf(profile1)),
            Arguments.of(ASC, "2", listOf(profile2)),
            Arguments.of(ASC, "3", listOf(profile3)),
            Arguments.of(DESC, "0", listOf(profile0)),
            Arguments.of(DESC, "1", listOf(profile1)),
            Arguments.of(DESC, "2", listOf(profile2)),
            Arguments.of(DESC, "3", listOf(profile3)),
            Arguments.of(ASC, "QWERTY", emptyList<EventProfileModel>()),
            Arguments.of(DESC, "QWERTY", emptyList<EventProfileModel>()),
        )

        @JvmStatic
        fun `Should updateEventProfileById throw RegistryException`(): Stream<Arguments> = Stream.of(
            Arguments.of(listOf(OTHER_EVENT_ROLE), EVENT_PROFILE_UPDATE_ROLE_HIGHER_THAN_CURRENT_USER),
            Arguments.of(listOf(EVENT_ROLE), EVENT_PROFILE_ASSIGNS_ROLE_HIGHER_THAN_CURRENT_USER),
        )
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findEventProfilesByEventId return Event's Profile`(
        order: Direction,
        searched: String?,
        expectedList: List<EventProfileModel>,
    ) {
        // Arrange
        `when`(
            repository.findEventProfilesByEventId(
                any(),
                any(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Flux.just(*profiles))

        // Act
        val result = service.findEventProfilesByEventId(
            eventId,
            order,
            onlyVisible = true,
            status = ACCEPTED,
            searched,
            startAccess = null,
            endAccess = null
        ).collectList().block()

        // Assert
        assertEquals(expectedList.size, result?.size)
        expectedList.forEachIndexed { index, it ->
            assertEquals(it, result?.get(index))
        }

        verify(repository).findEventProfilesByEventId(
            eventId,
            onlyVisible = true,
            onlyUsable = false,
            status = ACCEPTED,
            startAccess = null,
            endAccess = null
        )
    }

    @Test
    fun `Should findEventProfileByEventIdAndId return the Profile`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(
            repository.findById(
                any(),
                any(),
                any()
            )
        ).thenReturn(Mono.just(profile0))

        // Act
        service.findEventProfileByEventIdAndId(eventId, uuid, onlyVisible = true).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = true)
    }

    @Test
    fun `Should searchUsers return the searched User`() {
        // Arrange
        val searched = "John"
        `when`(userService.findUsers(any(), any(), any())).thenReturn(Flux.empty())

        // Act
        service.searchUsers(searched).collectList().block()

        // Assert
        verify(userService, times(1)).findUsers(order = ASC, onlyVisible = true, searched)
    }

    @Test
    fun `Should getAssignableEventRoles return the list of assignable role`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val currentUser = currentUser().apply { id = uuid }
        `when`(
            repository.findEventProfileByEventAndUserId(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(Mono.just(profile0))
        `when`(roleService.getAssignableEventRoles(any())).thenReturn(listOf(EVENT_ROLE))

        // Act
        service.getAssignableEventRoles(currentUser, eventId).blockFirst()

        // Assert
        verify(repository, times(1)).findEventProfileByEventAndUserId(
            eventId,
            uuid,
            onlyVisible = true,
            onlyUsable = true,
            status = ACCEPTED,
        )
        verify(roleService, times(1)).getAssignableEventRoles(profile0)
    }

    @Test
    fun `Should createSupportEventProfile create temporary Profile`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val currentUser = currentUser().apply { id = uuid }
        `when`(
            repository.findEventProfilesByEventId(
                any(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Flux.empty())
        `when`(roleService.getLevel0RoleFromEventRoles()).thenReturn(EVENT_ROLE)
        `when`(repository.create(any())).thenReturn(Mono.just(profile0))

        // Act
        service.createSupportEventProfile(currentUser, eventId).block()

        // Assert
        verify(repository, times(1)).findEventProfilesByEventId(
            eq(eventId),
            onlyVisible = eq(false),
            onlyUsable = eq(false),
            status = eq(null),
            startAccess = any(),
            endAccess = any()
        )
        verify(roleService, times(1)).getLevel0RoleFromEventRoles()
        verify(repository, times(1)).create(any())
    }

    @Test
    fun `Should createEventProfiles create and return Profiles`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val profiles = listOf(EventProfileModel())
        `when`(
            eventService.validateDateTimes(
                any(),
                anyOrNull(),
                anyOrNull(),
                any(),
            )
        ).thenReturn(Mono.just(UUID.randomUUID()))
        `when`(
            repository.findEventProfilesByEventId(
                any(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Flux.empty())
        `when`(repository.saveAll(any())).thenReturn(Flux.just(profile0))

        // Act
        service.createEventProfiles(currentUser(), eventId, listOf(uuid), profiles).block()

        // Assert
        verify(repository, times(1)).findEventProfilesByEventId(
            eventId,
            onlyVisible = false,
            onlyUsable = false,
            status = null,
            startAccess = null,
            endAccess = null
        )
        verify(repository, times(1)).saveAll(any())
    }

    @Test
    fun `Should createEventProfiles succeed partially`() {
        // Arrange
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val profiles = listOf(EventProfileModel().apply {
            user = UserModel().apply { id = uuid1 }
            role = EVENT_ROLE; event = EventModel().apply { name = "0" }
        })
        `when`(
            eventService.validateDateTimes(
                any(),
                anyOrNull(),
                anyOrNull(),
                any(),
            )
        ).thenReturn(Mono.just(UUID.randomUUID()))
        `when`(
            repository.findEventProfilesByEventId(
                any(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(
            Flux.just(
                EventProfileModel().apply { user = UserModel().apply { id = uuid1 }; status = REJECTED },
                EventProfileModel().apply { user = UserModel().apply { id = uuid1 }; status = ACCEPTED },
            )
        )
        `when`(repository.saveAll(any())).thenReturn(Flux.just(EventProfileModel().apply { user = UserModel().apply { id = uuid2 } }))

        // Act
        val result = service.createEventProfiles(currentUser(), eventId, listOf(uuid1, uuid2), profiles).block()

        // Assert
        assertEquals(1, result?.first?.size)
        verify(repository, times(1)).findEventProfilesByEventId(
            eventId,
            onlyVisible = false,
            onlyUsable = false,
            status = null,
            startAccess = null,
            endAccess = null
        )
        verify(repository, times(1)).saveAll(any())
    }

    @Test
    fun `Should createEventProfiles failed due to date conflict`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val profiles = listOf(EventProfileModel().apply {
            user = UserModel().apply { id = uuid }
            role = EVENT_ROLE; event = EventModel().apply { name = "0" }
        })
        `when`(
            eventService.validateDateTimes(
                any(),
                anyOrNull(),
                anyOrNull(),
                any(),
            )
        ).thenReturn(Mono.just(UUID.randomUUID()))
        `when`(
            repository.findEventProfilesByEventId(
                any(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(
            Flux.just(
                EventProfileModel().apply { user = UserModel().apply { id = uuid }; status = REJECTED },
                EventProfileModel().apply { user = UserModel().apply { id = uuid }; status = ACCEPTED },
            )
        )
        `when`(repository.saveAll(any())).thenReturn(Flux.just(profile0))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.createEventProfiles(currentUser(), eventId, listOf(uuid), profiles).block()
        }) as RegistryException

        // Assert
        assertEquals(CONFLICT, result.status)
        assertEquals(EVENT_PROFILE_ALREADY_EXIST_ON_RANGE, result.message)
        verify(repository, times(1)).findEventProfilesByEventId(
            eventId,
            onlyVisible = false,
            onlyUsable = false,
            status = null,
            startAccess = null,
            endAccess = null
        )
        verify(repository, times(0)).saveAll(any())
    }

    @Test
    fun `Should updateEventProfileById update and return Event's Profile`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val profileId = UUID.randomUUID()
        val currentUser = currentUser().apply { id = uuid }
        val userProfile = EventProfileModel()
        val profile = EventProfileModel().apply {
            user = UserModel().apply { id = uuid }
            event = EventModel().apply { id = eventId }
            role = EVENT_ROLE
        }
        `when`(
            eventService.validateDateTimes(
                any(),
                anyOrNull(),
                anyOrNull(),
                any(),
            )
        ).thenReturn(Mono.just(UUID.randomUUID()))
        `when`(
            repository.findEventProfilesByEventId(
                any(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Flux.empty())
        `when`(
            repository.findById(
                any(),
                any(),
                any()
            )
        ).thenReturn(Mono.just(profile))
        `when`(
            repository.findEventProfileByEventAndUserId(
                any(),
                any(),
                any(),
                any(),
                anyOrNull(),
            )
        ).thenReturn(Mono.just(userProfile))
        `when`(
            profileService.validateNotLastEventRoleLevel0(
                any(),
                any(),
                any(),
                any(),
            )
        ).thenReturn(Mono.just(profile))
        `when`(roleService.getAssignableEventRoles(any())).thenReturn(listOf(EVENT_ROLE))
        `when`(repository.update(any())).thenReturn(Mono.just(profile))

        // Act
        service.updateEventProfileById(currentUser, eventId, profileId, profile).block()

        // Assert
        verify(repository, times(1)).findEventProfilesByEventId(
            eventId,
            onlyVisible = false,
            onlyUsable = false,
            status = null,
            startAccess = null,
            endAccess = null
        )
        verify(repository, times(1)).findById(eventId, profileId, onlyVisible = false)
        verify(repository, times(1)).findEventProfileByEventAndUserId(
            eventId,
            uuid,
            onlyVisible = true,
            onlyUsable = true,
            status = ACCEPTED,
        )
        verify(roleService, times(1)).getAssignableEventRoles(userProfile)
        verify(repository, times(1)).update(profile)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should updateEventProfileById throw RegistryException`(
        assignableRoles: List<String>,
        message: String,
    ) {
        // Arrange
        val uuid = UUID.randomUUID()
        val profileId = UUID.randomUUID()
        val currentUser = currentUser().apply { id = uuid }
        val userProfile = EventProfileModel()
        val currentProfile = EventProfileModel().apply { user = currentUser; role = EVENT_ROLE }
        val nextProfile = EventProfileModel().apply { user = currentUser; role = OTHER_EVENT_ROLE }
        `when`(
            eventService.validateDateTimes(
                any(),
                anyOrNull(),
                anyOrNull(),
                any(),
            )
        ).thenReturn(Mono.just(UUID.randomUUID()))
        `when`(
            repository.findEventProfilesByEventId(
                any(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Flux.empty())
        `when`(
            repository.findById(
                any(),
                any(),
                any()
            )
        ).thenReturn(Mono.just(currentProfile))
        `when`(
            repository.findEventProfileByEventAndUserId(
                any(),
                any(),
                any(),
                any(),
                anyOrNull(),
            )
        ).thenReturn(Mono.just(userProfile))
        `when`(roleService.getAssignableEventRoles(any())).thenReturn(assignableRoles)

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.updateEventProfileById(currentUser, eventId, profileId, nextProfile).block()
        }) as RegistryException

        // Assert
        assertEquals(FORBIDDEN, result.status)
        assertEquals(message, result.message)

        verify(repository, times(1)).findEventProfilesByEventId(
            eventId,
            onlyVisible = false,
            onlyUsable = false,
            status = null,
            startAccess = null,
            endAccess = null,
        )
        verify(repository, times(1)).findById(eventId, profileId, onlyVisible = false)
        verify(repository, times(1)).findEventProfileByEventAndUserId(
            eventId,
            uuid,
            onlyVisible = true,
            onlyUsable = true,
            status = ACCEPTED,
        )
        verify(roleService, times(1)).getAssignableEventRoles(userProfile)
        verify(repository, never()).create(any())
    }

    @Test
    fun `Should blockEventProfileById hide and return a Profile`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val profile = EventProfileModel().apply {
            user = UserModel().apply { id = uuid }
            event = EventModel().apply { id = eventId }
        }
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(profile))
        `when`(repository.update(any())).thenReturn(Mono.just(profile))
        `when`(
            profileService.validateNotLastEventRoleLevel0(
                any(),
                any(),
                any(),
                any(),
            )
        ).thenReturn(Mono.just(profile))

        // Act
        service.blockEventProfileById(currentUser(), eventId, uuid).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = true)
        verify(repository, times(1)).update(any())
    }

    @Test
    fun `Should unblockEventProfileById restore and return a Profile`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(profile0))
        `when`(repository.update(any())).thenReturn(Mono.just(profile0))

        // Act
        service.unblockEventProfileById(currentUser(), eventId, uuid).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = false)
        verify(repository, times(1)).update(any())
    }

    @Test
    fun `Should deleteEventProfileById delete a Profile`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val profile = EventProfileModel().apply {
            id = uuid
            user = UserModel().apply { id = uuid }
            event = EventModel().apply { id = eventId }
        }
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(profile))
        `when`(repository.deleteById(any())).thenReturn(Mono.empty())
        `when`(
            profileService.validateNotLastEventRoleLevel0(
                any(),
                any(),
                any(),
                any(),
            )
        ).thenReturn(Mono.just(profile))

        // Act
        service.deleteEventProfileById(currentUser(), eventId, uuid).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible = false)
        verify(repository, times(1)).deleteById(uuid)
    }
}
