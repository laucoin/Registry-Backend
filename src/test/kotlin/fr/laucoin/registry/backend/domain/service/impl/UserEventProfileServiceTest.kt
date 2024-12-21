package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.EventProfileRoleCountModel
import fr.laucoin.registry.backend.domain.model.RegistryExceptionModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.repository.IEventProfileModelRepository
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserEventProfileService
import fr.laucoin.registry.backend.test.ModelExt.eventId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.test.util.ReflectionTestUtils.setField
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class UserEventProfileServiceTest {
    private val repository: IEventProfileModelRepository = mock()
    private val roleService: IRoleService = mock()
    private val service: IUserEventProfileService = UserEventProfileService(repository, roleService)

    companion object {
        private val profile0 = EventProfileModel().apply { event = EventModel().apply { name = "0" } }
        private val profile1 = EventProfileModel().apply { event = EventModel().apply { name = "1" } }
        private val profile2 = EventProfileModel().apply { event = EventModel().apply { name = "2" } }
        private val profile3 = EventProfileModel().apply { event = EventModel().apply { name = "3" } }

        private val profiles = arrayOf(profile0, profile1, profile2, profile3)

        @JvmStatic
        fun `Should findUserEventProfiles return User's Event Profile`(): Stream<Arguments> = Stream.of(
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
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findUserEventProfiles return User's Event Profile`(
        order: Direction,
        searched: String?,
        expectedList: List<EventProfileModel>,
    ) {
        // Arrange
        setField(service, "searchThreshold", 0.5)
        val uuid = UUID.randomUUID()
        `when`(
            repository.findEventProfilesByUserId(
                any(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Flux.just(*profiles))

        // Act
        val result = service.findUserEventProfiles(
            uuid,
            order,
            onlyVisible = true,
            onlyUsable = true,
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

        verify(repository).findEventProfilesByUserId(
            uuid,
            onlyVisible = true,
            onlyUsable = true,
            status = ACCEPTED,
            startAccess = null,
            endAccess = null
        )
    }

    @Test
    fun `Should findEventByBlockingLevel0RoleAndUserId return the level 0 Profile`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val input = arrayOf(
            EventProfileRoleCountModel(level0 = null),
            EventProfileRoleCountModel(level0 = 0),
            EventProfileRoleCountModel(level0 = 1),
            EventProfileRoleCountModel(level0 = 2),
        )
        val expectedResult = arrayOf(input[0], input[1], input[2])
        `when`(repository.findLevel0EventProfileRoleByUserId(any(), any())).thenReturn(Flux.just(*input))

        // Act
        val result = service.findEventByBlockingLevel0RoleAndUserId(uuid, onlyVisible = true).collectList().block()

        // Assert
        assertEquals(expectedResult.size, result?.size)
        expectedResult.forEach {
            assertTrue(result !!.contains(it))
        }

        verify(repository).findLevel0EventProfileRoleByUserId(uuid, onlyVisible = true)
    }

    @Test
    fun `Should findUserEventProfileById return the Profile`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val currentUser = currentUser()
        `when`(repository.findEventProfilesByIdAndUserId(any(), any(), any())).thenReturn(Mono.just(profile0))

        // Act
        service.findUserEventProfileById(currentUser, uuid, onlyVisible = true).block()

        // Assert
        verify(repository, times(1)).findEventProfilesByIdAndUserId(currentUser.id !!, uuid, onlyVisible = true)
    }

    @Test
    fun `Should validateNotLastEventRoleLevel0 return the given Object`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val user = UserModel()
        `when`(
            repository.findLevel0EventProfileRoleByUserId(
                any(),
                any()
            )
        ).thenReturn(Flux.empty())

        // Act
        val result = service.validateNotLastEventRoleLevel0(uuid, eventId = null, user, error = "ERROR_MESSAGE").block()

        // Assert
        assertEquals(user, result)
        verify(repository, times(1)).findLevel0EventProfileRoleByUserId(uuid, onlyVisible = true)
    }

    @Test
    fun `Should validateNotLastEventRoleLevel0 throw RegistryExceptionModel`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val errorMessage = "ERROR_MESSAGE"
        `when`(
            repository.findLevel0EventProfileRoleByUserId(
                any(),
                any()
            )
        ).thenReturn(Flux.just(EventProfileRoleCountModel(level0 = 0)))

        // Act
        val result = assertThrows(RegistryExceptionModel::class.java) {
            service.validateNotLastEventRoleLevel0(uuid, eventId = null, UserModel(), errorMessage).block()
        }

        // Assert
        assertEquals(FORBIDDEN, result.status)
        assertEquals(errorMessage, result.message)

        verify(repository, times(1)).findLevel0EventProfileRoleByUserId(uuid, onlyVisible = true)
    }

    @Test
    fun `Should createUserEventProfileFromEvent create and return a Profile`() {
        // Arrange
        `when`(repository.create(any())).thenReturn(Mono.just(profile0))

        // Act
        service.createUserEventProfileFromEvent(currentUser(), EventModel()).block()

        // Assert
        verify(repository, times(1)).create(any())
    }

    @Test
    fun `Should updateUserEventProfileStatusById update Event Profile status is INVITED`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val currentUser = currentUser()
        `when`(repository.findEventProfilesByIdAndUserId(any(), any(), any())).thenReturn(Mono.just(profile0.apply {
            status = INVITED
        }))
        `when`(repository.update(any())).thenReturn(Mono.just(profile0))

        // Act
        service.updateUserEventProfileStatusById(currentUser, uuid, ACCEPTED).block()

        // Assert
        verify(repository, times(1)).update(any())
        verify(repository, times(1)).findEventProfilesByIdAndUserId(currentUser.id !!, uuid, onlyVisible = true)
    }

    @Test
    fun `Should updateUserEventProfileStatusById throw RegistryExceptionModel`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val currentUser = currentUser()
        `when`(repository.findEventProfilesByIdAndUserId(any(), any(), any())).thenReturn(Mono.just(profile0.apply {
            status = ACCEPTED
        }))
        `when`(repository.update(any())).thenReturn(Mono.just(profile0))

        // Act
        val result = assertThrows(RegistryExceptionModel::class.java) {
            service.updateUserEventProfileStatusById(currentUser, uuid, ACCEPTED).block()
        }

        // Assert
        assertEquals(NOT_FOUND, result.status)
        assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
        assertEquals(uuid.toString(), result.args?.get("identifier"))

        verify(repository, never()).update(any())
        verify(repository, times(1)).findEventProfilesByIdAndUserId(currentUser.id !!, uuid, onlyVisible = true)
    }

    @Test
    fun `Should deleteUserEventProfileById delete Profile`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val currentUser = currentUser()
        `when`(
            repository.findLevel0EventProfileRoleByUserId(
                any(),
                any()
            )
        ).thenReturn(Flux.empty())
        `when`(repository.findEventProfilesByIdAndUserId(any(), any(), any())).thenReturn(Mono.just(profile0.apply {
            user = UserModel().apply { id = currentUser.id }
            event = EventModel().apply { id = eventId }
        }))
        `when`(repository.deleteById(any())).thenReturn(Mono.empty())

        // Act
        service.deleteUserEventProfileById(currentUser, uuid).block()

        // Assert
        verify(repository, times(1)).findEventProfilesByIdAndUserId(currentUser.id !!, uuid, onlyVisible = false)
        verify(repository, times(1)).deleteById(uuid)
    }
}
