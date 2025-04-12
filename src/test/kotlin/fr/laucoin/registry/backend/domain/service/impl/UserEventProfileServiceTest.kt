package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_ALREADY_EXIST_ON_RANGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.EventProfileRoleCountModel
import fr.laucoin.registry.backend.domain.model.EventProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.repository.IEventProfileModelRepository
import fr.laucoin.registry.backend.domain.repository.IPreferencesModelRepository
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserEventProfileService
import fr.laucoin.registry.backend.test.ModelExt.eventId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.util.Objects
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class UserEventProfileServiceTest {
    private val repository: IEventProfileModelRepository = mock()
    private val roleService: IRoleService = mock()
    private val preferencesRepository: IPreferencesModelRepository = mock()
    private val transactionalOperator: TransactionalOperator = mock()
    private val service: IUserEventProfileService =
        UserEventProfileService(repository, roleService, preferencesRepository, transactionalOperator)

    companion object {
        @JvmStatic
        fun `Should validateNotLastEventRoleLevel0 return the given Object`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(null, null),
                Arguments.of(eventId, EventProfileRoleCountModel(level0 = 0, event = EventModel().apply { id = eventId })),
            )
        }

        @JvmStatic
        fun `Should validateNotLastEventRoleLevel0 throw RegistryException`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(eventId, EventProfileRoleCountModel(level0 = 0)),
                Arguments.of(eventId, EventProfileRoleCountModel(level0 = 0, event = EventModel().apply { id = UUID.randomUUID() })),
                Arguments.of(null, EventProfileRoleCountModel(level0 = 0)),
                Arguments.of(null, EventProfileRoleCountModel(level0 = 0, event = EventModel().apply { id = UUID.randomUUID() })),
            )
        }

        @JvmStatic
        fun `Should createUserEventProfileFromEvent create and return a Profile`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(PreferencesModel(), 1),
                Arguments.of(PreferencesModel().apply { selectedProfile = EventProfileModel() }, 0),
            )
        }
    }

    @Test
    fun `Should findEventProfilesPage call repository findEventProfilesPageByUserId`() {
        // Arrange
        val pageable = PageableModel(0, 10)
        val params = EventProfileSearchParamModel(statusSearched = ACCEPTED)
        whenever(repository.findEventProfilesPageByUserId(any(), any(), any())).thenReturn(
            Mono.just(PageModel(1, 2, 3, 4, emptyList()))
        )

        // Act
        service.findEventProfilesPage(eventId, pageable, params).block()

        // Assert
        verify(repository).findEventProfilesPageByUserId(eventId, pageable, params)
    }

    @Test
    fun `Should findUserEventProfileById call repository findEventProfileByUserIdAndId`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val profile = EventProfileModel().apply {
            id = uuid
            event = EventModel().apply { id = eventId }
        }
        val onlyVisible = true
        whenever(repository.findEventProfileByUserIdAndId(any(), any(), anyOrNull())).thenReturn(Mono.just(profile))

        // Act
        service.findUserEventProfileById(currentUser(), uuid, onlyVisible).block()

        // Assert
        verify(repository).findEventProfileByUserIdAndId(currentUser().id !!, uuid, onlyVisible)
    }

    @Test
    fun `Should findUserEventProfileById call repository findEventProfileByUserIdAndId throw on empty result`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val onlyVisible = true
        whenever(repository.findEventProfileByUserIdAndId(any(), any(), anyOrNull())).thenReturn(Mono.empty())

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.findUserEventProfileById(currentUser(), uuid, onlyVisible).block()
        }) as RegistryException

        // Assert
        assertEquals(NOT_FOUND, result.status)
        assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
        assertEquals(1, result.args?.size)
        verify(repository).findEventProfileByUserIdAndId(currentUser().id !!, uuid, onlyVisible)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should validateNotLastEventRoleLevel0 return the given Object`(
        eventId: UUID?,
        profileCount: EventProfileRoleCountModel?
    ) {
        // Arrange
        val uuid = UUID.randomUUID()
        val user = UserModel()
        whenever(
            repository.findLevel0EventProfileRoleByUserId(
                any(),
                any()
            )
        ).thenReturn(if (Objects.nonNull(profileCount)) Flux.just(profileCount !!) else Flux.empty())

        // Act
        val result = service.validateNotLastEventRoleLevel0(uuid, eventId = eventId, user, error = "ERROR_MESSAGE").block()

        // Assert
        assertEquals(user, result)
        verify(repository).findLevel0EventProfileRoleByUserId(uuid, visibilitySearched = true)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should validateNotLastEventRoleLevel0 throw RegistryException`(
        eventId: UUID?,
        profileCount: EventProfileRoleCountModel
    ) {
        // Arrange
        val uuid = UUID.randomUUID()
        val errorMessage = "ERROR_MESSAGE"
        whenever(
            repository.findLevel0EventProfileRoleByUserId(
                any(),
                any()
            )
        ).thenReturn(Flux.just(profileCount))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.validateNotLastEventRoleLevel0(uuid, eventId = eventId, UserModel(), errorMessage).block()
        }) as RegistryException

        // Assert
        assertEquals(FORBIDDEN, result.status)
        assertEquals(errorMessage, result.message)

        verify(repository).findLevel0EventProfileRoleByUserId(uuid, visibilitySearched = true)
    }

    @Test
    fun `Should createSupportEventProfile call repository findUserIdsWithEventProfile and create`() {
        // Arrange
        val profileRole = "EVENT_ROLE"
        val profile = EventProfileModel().apply {
            user = currentUser()
            event = EventModel().apply { id = eventId }
            role = profileRole
        }
        whenever(roleService.getLevel0RoleFromEventRoles()).thenReturn(profileRole)
        whenever(
            repository.findUserIdsWithEventProfileForEventWithProfileExclusion(
                any(),
                any(),
                anyOrNull(),
                any(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Flux.empty())
        whenever(repository.create(any())).thenReturn(Mono.just(profile))

        // Act
        val result = service.createSupportEventProfile(currentUser(), eventId).block()

        // Assert
        assertEquals(profile, result)
        verify(repository).findUserIdsWithEventProfileForEventWithProfileExclusion(
            eq(eventId),
            eq(listOf(currentUser().id !!)),
            eq(null),
            eq(listOf(ACCEPTED, INVITED)),
            any(),
            any(),
        )
        verify(repository).create(any())
    }

    @Test
    fun `Should createSupportEventProfile call repository findUserIdsWithEventProfile and throw because profile duplicated`() {
        // Arrange
        val profileRole = "EVENT_ROLE"
        val profile = EventProfileModel().apply {
            user = currentUser()
            event = EventModel().apply { id = eventId }
            role = profileRole
        }
        whenever(roleService.getLevel0RoleFromEventRoles()).thenReturn(profileRole)
        whenever(
            repository.findUserIdsWithEventProfileForEventWithProfileExclusion(
                any(),
                any(),
                anyOrNull(),
                any(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Flux.just(currentUser().id !!))
        whenever(repository.create(any())).thenReturn(Mono.just(profile))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.createSupportEventProfile(currentUser(), eventId).block()
        }) as RegistryException

        // Assert
        assertEquals(CONFLICT, result.status)
        assertEquals(EVENT_PROFILE_ALREADY_EXIST_ON_RANGE, result.message)
        verify(repository).findUserIdsWithEventProfileForEventWithProfileExclusion(
            eq(eventId),
            eq(listOf(currentUser().id !!)),
            eq(null),
            eq(listOf(ACCEPTED, INVITED)),
            any(),
            any(),
        )
        verify(repository, never()).create(any())
    }

    @ParameterizedTest
    @MethodSource
    fun `Should createUserEventProfileFromEvent create and return a Profile`(
        userPreferences: PreferencesModel,
        expectedCallToUpdatePreferences: Int
    ) {
        // Arrange
        val uuid = UUID.randomUUID()
        val profile = EventProfileModel().apply {
            id = uuid
            event = EventModel().apply { id = eventId }
        }
        whenever(repository.create(any())).thenReturn(Mono.just(profile))
        whenever(preferencesRepository.findByUserId(any(), anyOrNull())).thenReturn(Mono.just(userPreferences))
        whenever(preferencesRepository.save(any())).thenReturn(Mono.just(userPreferences))
        whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

        // Act
        service.createUserEventProfileFromEvent(currentUser(), EventModel()).block()

        // Assert
        verify(repository).create(any())
        verify(preferencesRepository).findByUserId(currentUser().id !!, visibilitySearched = null)
        verify(preferencesRepository, times(expectedCallToUpdatePreferences)).save(any())
        verify(transactionalOperator).transactional(any<Mono<*>>())
    }

    @Test
    fun `Should updateUserEventProfileStatusById update Event Profile status is INVITED`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val profile = EventProfileModel().apply {
            id = uuid
            event = EventModel().apply { id = eventId }
            status = INVITED
        }
        whenever(repository.findEventProfileByUserIdAndId(any(), any(), any())).thenReturn(Mono.just(profile))
        whenever(repository.update(any())).thenReturn(Mono.just(profile))

        // Act
        service.updateUserEventProfileStatusById(currentUser(), uuid, ACCEPTED).block()

        // Assert
        verify(repository).update(any())
        verify(repository).findEventProfileByUserIdAndId(currentUser().id !!, uuid, visibilitySearched = true)
    }

    @Test
    fun `Should updateUserEventProfileStatusById throw RegistryException`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val profile = EventProfileModel().apply {
            id = uuid
            event = EventModel().apply { id = eventId }
            status = ACCEPTED
        }
        whenever(repository.findEventProfileByUserIdAndId(any(), any(), any())).thenReturn(Mono.just(profile))
        whenever(repository.update(any())).thenReturn(Mono.just(profile))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.updateUserEventProfileStatusById(currentUser(), uuid, ACCEPTED).block()
        }) as RegistryException

        // Assert
        assertEquals(NOT_FOUND, result.status)
        assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
        assertEquals(uuid.toString(), result.args?.first())

        verify(repository, never()).update(any())
        verify(repository).findEventProfileByUserIdAndId(currentUser().id !!, uuid, visibilitySearched = true)
    }

    @Test
    fun `Should deleteUserEventProfileById delete Profile`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val profile = EventProfileModel().apply {
            id = uuid
            user = currentUser()
            event = EventModel().apply { id = eventId }
        }
        whenever(
            repository.findLevel0EventProfileRoleByUserId(
                any(),
                any()
            )
        ).thenReturn(Flux.empty())
        whenever(repository.findEventProfileByUserIdAndId(any(), any(), anyOrNull())).thenReturn(Mono.just(profile))
        whenever(repository.deleteById(any())).thenReturn(Mono.empty())

        // Act
        service.deleteUserEventProfileById(currentUser(), uuid).block()

        // Assert
        verify(repository).findEventProfileByUserIdAndId(currentUser().id !!, uuid, visibilitySearched = null)
        verify(repository).deleteById(uuid)
    }
}
