package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_ALREADY_EXIST_ON_RANGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_ASSIGNS_ROLE_HIGHER_THAN_CURRENT_USER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_BLOCK_LAST_EVENT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_DELETE_LAST_EVENT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_UPDATE_LAST_EVENT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_UPDATE_ROLE_HIGHER_THAN_CURRENT_USER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.EventProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.repository.IEventProfileModelRepository
import fr.laucoin.registry.backend.domain.repository.IUserModelRepository
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserEventProfileService
import fr.laucoin.registry.backend.test.ModelExt.eventId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class EventProfileServiceTest {
    private val repository: IEventProfileModelRepository = mock()
    private val profileService: IUserEventProfileService = mock()
    private val roleService: IRoleService = mock()
    private val userRepository: IUserModelRepository = mock()
    private val maxUser: Int = 1
    private val service = EventProfileService(profileService, repository, roleService, userRepository, maxUser)

    companion object {
        @JvmStatic
        fun `Should createEventProfiles call repository findUserIdsWithEventProfile and saveAll`(): Stream<Arguments> {
            val uuid1 = UUID.randomUUID()
            val uuid2 = UUID.randomUUID()
            return Stream.of(
                Arguments.of(listOf(uuid1, uuid2), emptyList<UUID>(), listOf(uuid1, uuid2)),
                Arguments.of(listOf(uuid1, uuid2), listOf(uuid2), listOf(uuid1)),
            )
        }

        @JvmStatic
        fun `Should updateEventProfileById call repository findById, findUserIdsWithEventProfile and throw because user are not allowed to edit that role`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    "INITIAL_ROLE_EVENT",
                    "UPDATED_ROLE_EVENT",
                    "UPDATED_ROLE_EVENT",
                    EVENT_PROFILE_UPDATE_ROLE_HIGHER_THAN_CURRENT_USER
                ),
                Arguments.of(
                    "INITIAL_ROLE_EVENT",
                    "UPDATED_ROLE_EVENT",
                    "INITIAL_ROLE_EVENT",
                    EVENT_PROFILE_ASSIGNS_ROLE_HIGHER_THAN_CURRENT_USER
                ),
            )
        }
    }

    @Test
    fun `Should findEventProfilesPage call repository findEventProfilesPageByEventId`() {
        // Arrange
        val pageable = PageableModel(0, 10)
        val params = EventProfileSearchParamModel(statusSearched = ACCEPTED)
        whenever(repository.findEventProfilesPageByEventId(any(), any(), any())).thenReturn(
            Mono.just(PageModel(1, 2, 3, 4, emptyList()))
        )

        // Act
        service.findEventProfilesPage(eventId, pageable, params).block()

        // Assert
        verify(repository).findEventProfilesPageByEventId(eventId, pageable, params)
    }

    @Test
    fun `Should findEventProfileById call repository findById`() {
        // Arrange
        val profile = EventProfileModel().apply {
            role = "EVENT_ROLE"; event = EventModel().apply { id = eventId }
        }
        val uuid = UUID.randomUUID()
        val onlyVisible = true
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(profile))

        // Act
        service.findEventProfileById(eventId, uuid, onlyVisible).block()

        // Assert
        verify(repository).findById(eventId, uuid, onlyVisible)
    }

    @Test
    fun `Should findEventProfileById call repository findById throw on empty result`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val onlyVisible = true
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.empty())

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.findEventProfileById(eventId, uuid, onlyVisible).block()
        }) as RegistryException

        // Assert
        assertEquals(NOT_FOUND, result.status)
        assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
        assertEquals(1, result.args?.size)
        verify(repository).findById(eventId, uuid, onlyVisible)
    }

    @Test
    fun `Should searchUsers call repository findWithLimit`() {
        // Arrange
        val text = "text"
        whenever(userRepository.findWithLimit(any(), any())).thenReturn(Flux.empty())

        // Act
        service.searchUsers(text).blockFirst()

        // Assert
        verify(userRepository).findWithLimit(eq(maxUser), eq(UserSearchParamModel(text, visibilitySearched = true)))
    }

    @Test
    fun `Should getAssignableEventRoles call repository findEventProfileByEventAndUserId and role service getAssignableEventRoles`() {
        // Arrange
        val profile = EventProfileModel().apply {
            role = "EVENT_ROLE"; event = EventModel().apply { id = eventId }
        }
        whenever(repository.findEventProfileByEventAndUserId(any(), any(), anyOrNull())).thenReturn(Mono.just(profile))
        whenever(roleService.getAssignableEventRoles(any())).thenReturn(emptyList())

        // Act
        service.getAssignableEventRoles(currentUser(), eventId).blockFirst()

        // Assert
        verify(repository).findEventProfileByEventAndUserId(
            eventId,
            currentUser().id !!,
            EventProfileSearchParamModel(
                visibilitySearched = true,
                availabilitySearched = true,
                statusSearched = listOf(ACCEPTED),
            ),
        )
        verify(roleService).getAssignableEventRoles(profile)
    }

    @Test
    fun `Should getAssignableEventRoles throw on repository findEventProfileByEventAndUserId return empty`() {
        // Arrange
        whenever(repository.findEventProfileByEventAndUserId(any(), any(), anyOrNull())).thenReturn(Mono.empty())
        whenever(roleService.getAssignableEventRoles(any())).thenReturn(emptyList())

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.getAssignableEventRoles(currentUser(), eventId).blockFirst()
        }) as RegistryException

        // Assert
        assertEquals(NOT_FOUND, result.status)
        assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
        assertEquals(1, result.args?.size)
        verify(repository).findEventProfileByEventAndUserId(
            eventId,
            currentUser().id !!,
            EventProfileSearchParamModel(
                visibilitySearched = true,
                availabilitySearched = true,
                statusSearched = listOf(ACCEPTED),
            ),
        )
        verify(roleService, never()).getAssignableEventRoles(any())
    }

    @ParameterizedTest
    @MethodSource
    fun `Should createEventProfiles call repository findUserIdsWithEventProfile and saveAll`(
        wantedProfileForUserIds: List<UUID>,
        userIdWithExistingProfile: List<UUID>,
        expectedCreatedUserIds: List<UUID>,
    ) {
        // Arrange
        val profiles = wantedProfileForUserIds.map { EventProfileModel().apply { user = UserModel().apply { id = it } } }
        val expectedProfiles = profiles.filter { expectedCreatedUserIds.contains(it.user?.id) }
        whenever(
            repository.findUserIdsWithEventProfileForEventWithProfileExclusion(
                any(),
                any(),
                anyOrNull(),
                any(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Flux.just(*userIdWithExistingProfile.toTypedArray()))
        whenever(repository.saveAll(any())).thenReturn(Flux.just(*expectedProfiles.toTypedArray()))

        // Act
        val result = service.createEventProfiles(currentUser(), eventId, wantedProfileForUserIds, profiles).block()

        // Assert
        assertEquals(expectedCreatedUserIds, result?.first)
        assertEquals(userIdWithExistingProfile, result?.second)
        verify(repository).findUserIdsWithEventProfileForEventWithProfileExclusion(
            eventId,
            wantedProfileForUserIds,
            profileIdToExclude = null,
            statusSearched = listOf(ACCEPTED, INVITED),
            startDateTimeSearched = null,
            endDateTimeSearched = null,
        )
        verify(repository).saveAll(expectedProfiles)
    }

    @Test
    fun `Should createEventProfiles call repository findUserIdsWithEventProfile and throw because all profiles duplicated`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val users = listOf(uuid)
        val profile = EventProfileModel().apply {
            user = UserModel().apply { id = uuid }
            startAccess = CustomDateTimeModel(LocalDate.EPOCH)
            endAccess = CustomDateTimeModel(LocalDate.EPOCH)
        }
        val profiles = listOf(profile)
        whenever(
            repository.findUserIdsWithEventProfileForEventWithProfileExclusion(
                any(),
                any(),
                anyOrNull(),
                any(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Flux.just(uuid))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.createEventProfiles(currentUser(), eventId, users, profiles).block()
        }) as RegistryException

        // Assert
        assertEquals(CONFLICT, result.status)
        assertEquals(EVENT_PROFILE_ALREADY_EXIST_ON_RANGE, result.message)
        verify(repository).findUserIdsWithEventProfileForEventWithProfileExclusion(
            eventId,
            users,
            profileIdToExclude = null,
            statusSearched = listOf(ACCEPTED, INVITED),
            startDateTimeSearched = LocalDateTime.of(LocalDate.EPOCH, LocalTime.MIN),
            endDateTimeSearched = LocalDateTime.of(LocalDate.EPOCH, LocalTime.MAX),
        )
        verify(repository, never()).saveAll(any())
    }

    @Test
    fun `Should updateEventProfileById call repository findById, findUserIdsWithEventProfile, call profileService validateNotLastEventRoleLevel0, call repository findEventProfileByEventAndUserId, call roleService getAssignableEventRoles and finally call repository update`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val profileRole = "ROLE_EVENT"
        val currentUserProfile = EventProfileModel().apply {
            role = profileRole
            user = UserModel().apply { id = currentUser().id }
            event = EventModel().apply { id = eventId }
        }
        val profile = EventProfileModel().apply {
            role = profileRole
            user = UserModel().apply { id = uuid }
            event = EventModel().apply { id = eventId }
            startAccess = CustomDateTimeModel(LocalDate.EPOCH)
            endAccess = CustomDateTimeModel(LocalDate.EPOCH)
        }
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(profile))
        whenever(
            repository.findUserIdsWithEventProfileForEventWithProfileExclusion(
                any(),
                any(),
                anyOrNull(),
                any(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Flux.just())
        whenever(profileService.validateNotLastEventRoleLevel0(any(), any(), any(), any())).thenReturn(Mono.just(profile))
        whenever(repository.findEventProfileByEventAndUserId(any(), any(), any())).thenReturn(Mono.just(currentUserProfile))
        whenever(roleService.getAssignableEventRoles(any())).thenReturn(listOf(profileRole))
        whenever(repository.update(any())).thenReturn(Mono.just(profile))

        // Act
        val result = service.updateEventProfileById(currentUser(), eventId, uuid, profile).block()

        // Assert
        assertEquals(profile, result)
        verify(repository).findById(eventId, uuid, visibilitySearched = null)
        verify(repository).findUserIdsWithEventProfileForEventWithProfileExclusion(
            eventId,
            listOf(uuid),
            profileIdToExclude = null,
            statusSearched = listOf(ACCEPTED, INVITED),
            startDateTimeSearched = LocalDateTime.of(LocalDate.EPOCH, LocalTime.MIN),
            endDateTimeSearched = LocalDateTime.of(LocalDate.EPOCH, LocalTime.MAX),
        )
        verify(profileService).validateNotLastEventRoleLevel0(
            uuid,
            eventId,
            profile,
            EVENT_PROFILE_UPDATE_LAST_EVENT_ADMINISTRATOR
        )
        verify(repository).findEventProfileByEventAndUserId(
            eventId,
            currentUser().id !!,
            EventProfileSearchParamModel(
                visibilitySearched = true,
                availabilitySearched = true,
                statusSearched = listOf(ACCEPTED),
            ),
        )
        verify(roleService).getAssignableEventRoles(currentUserProfile)
        verify(repository).update(profile)
    }

    @Test
    fun `Should updateEventProfileById call repository findById, findUserIdsWithEventProfile, call profileService validateNotLastEventRoleLevel0, call repository findEventProfileByEventAndUserId, call roleService getAssignableEventRoles and throw because profile duplicated`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val profileRole = "ROLE_EVENT"
        val profile = EventProfileModel().apply {
            role = profileRole
            user = UserModel().apply { id = uuid }
            event = EventModel().apply { id = eventId }
        }
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(profile))
        whenever(
            repository.findUserIdsWithEventProfileForEventWithProfileExclusion(
                any(),
                any(),
                anyOrNull(),
                any(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Flux.just(uuid))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.updateEventProfileById(currentUser(), eventId, uuid, profile).block()
        }) as RegistryException

        // Assert
        assertEquals(CONFLICT, result.status)
        assertEquals(EVENT_PROFILE_ALREADY_EXIST_ON_RANGE, result.message)
        verify(repository).findById(eventId, uuid, visibilitySearched = null)
        verify(repository).findUserIdsWithEventProfileForEventWithProfileExclusion(
            eventId,
            listOf(uuid),
            profileIdToExclude = null,
            statusSearched = listOf(ACCEPTED, INVITED),
            startDateTimeSearched = null,
            endDateTimeSearched = null,
        )
        verify(profileService, never()).validateNotLastEventRoleLevel0(any(), any(), any(), any())
        verify(repository, never()).findEventProfileByEventAndUserId(any(), any(), any())
        verify(roleService, never()).getAssignableEventRoles(any())
        verify(repository, never()).update(any())
    }

    @ParameterizedTest
    @MethodSource
    fun `Should updateEventProfileById call repository findById, findUserIdsWithEventProfile and throw because user are not allowed to edit that role`(
        profileToUpdateRole: String,
        profileUpdatedRole: String,
        allowedRoleString: String,
        expectedErrorMessage: String,
    ) {
        // Arrange
        val uuid = UUID.randomUUID()
        val currentUserProfile = EventProfileModel().apply {
            role = "ROLE_EVENT"
            user = UserModel().apply { id = currentUser().id }
            event = EventModel().apply { id = eventId }
        }
        val profileToUpdate = EventProfileModel().apply {
            role = profileToUpdateRole
            user = UserModel().apply { id = uuid }
            event = EventModel().apply { id = eventId }
        }
        val profileUpdated = EventProfileModel().apply {
            role = profileUpdatedRole
            user = UserModel().apply { id = uuid }
            event = EventModel().apply { id = eventId }
        }
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(profileToUpdate))
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
        whenever(repository.findEventProfileByEventAndUserId(any(), any(), any())).thenReturn(Mono.just(currentUserProfile))
        whenever(roleService.getAssignableEventRoles(any())).thenReturn(listOf(allowedRoleString))


        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.updateEventProfileById(currentUser(), eventId, uuid, profileUpdated).block()
        }) as RegistryException

        // Assert
        assertEquals(FORBIDDEN, result.status)
        assertEquals(expectedErrorMessage, result.message)
        verify(repository).findById(eventId, uuid, visibilitySearched = null)
        verify(repository).findUserIdsWithEventProfileForEventWithProfileExclusion(
            eventId,
            listOf(uuid),
            profileIdToExclude = null,
            statusSearched = listOf(ACCEPTED, INVITED),
            startDateTimeSearched = null,
            endDateTimeSearched = null,
        )
        verify(profileService, never()).validateNotLastEventRoleLevel0(any(), anyOrNull(), any(), any())
        verify(repository).findEventProfileByEventAndUserId(
            eventId,
            currentUser().id !!,
            EventProfileSearchParamModel(
                visibilitySearched = true,
                availabilitySearched = true,
                statusSearched = listOf(ACCEPTED),
            ),
        )
        verify(roleService).getAssignableEventRoles(currentUserProfile)
        verify(repository, never()).update(any())
    }

    @Test
    fun `Should blockEventProfileById call repository findById, call service profile validateNotLastEventRoleLevel0 and finally call repository update`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val profile = EventProfileModel().apply {
            user = UserModel().apply { id = uuid }
            event = EventModel().apply { id = eventId }
            visible = true
        }
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(profile))
        whenever(profileService.validateNotLastEventRoleLevel0(any(), any(), any(), any())).thenReturn(Mono.just(profile))
        whenever(repository.update(any())).thenReturn(Mono.just(profile))

        // Act
        service.blockEventProfileById(currentUser(), eventId, uuid).block()

        // Assert
        verify(repository).findById(eventId, uuid, visibilitySearched = true)
        verify(profileService).validateNotLastEventRoleLevel0(
            uuid,
            eventId,
            profile,
            EVENT_PROFILE_BLOCK_LAST_EVENT_ADMINISTRATOR,
        )
        verify(repository).update(profile.apply { visible = false })
    }

    @Test
    fun `Should unblockEventProfileById call repository findById, call service profile validateNotLastEventRoleLevel0 and finally call repository update`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val profile = EventProfileModel().apply {
            user = UserModel().apply { id = uuid }
            event = EventModel().apply { id = eventId }
            visible = false
        }
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(profile))
        whenever(repository.update(any())).thenReturn(Mono.just(profile))

        // Act
        service.unblockEventProfileById(currentUser(), eventId, uuid).block()

        // Assert
        verify(repository).findById(eventId, uuid, visibilitySearched = false)
        verify(repository).update(profile.apply { visible = true })
    }

    @Test
    fun `Should deleteEventProfileById call repository findById, call service profile validateNotLastEventRoleLevel0 and finally call repository deleteById`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val profile = EventProfileModel().apply {
            user = UserModel().apply { id = uuid }
            event = EventModel().apply { id = eventId }
            visible = true
        }
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(profile))
        whenever(profileService.validateNotLastEventRoleLevel0(any(), any(), any(), any())).thenReturn(Mono.just(profile))
        whenever(repository.deleteById(any())).thenReturn(Mono.empty())

        // Act
        service.deleteEventProfileById(currentUser(), eventId, uuid).block()

        // Assert
        verify(repository).findById(eventId, uuid, visibilitySearched = null)
        verify(profileService).validateNotLastEventRoleLevel0(
            uuid,
            eventId,
            profile,
            EVENT_PROFILE_DELETE_LAST_EVENT_ADMINISTRATOR,
        )
        verify(repository).deleteById(uuid)
    }
}
