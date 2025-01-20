package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.repository.IPreferencesModelRepository
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import fr.laucoin.registry.backend.domain.service.IUserEventProfileService
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
import org.springframework.http.HttpStatus.NOT_FOUND
import reactor.core.publisher.Mono

class PreferencesServiceTest {
    private val repository: IPreferencesModelRepository = mock()
    private val eventProfileService: IUserEventProfileService = mock()
    private val service: IPreferencesService = PreferencesService(repository, eventProfileService)

    companion object {
        @JvmStatic
        fun `Should findByUser return the User's Preferences`(): Stream<Arguments> = Stream.of(
            Arguments.of(false, 1, 0),
            Arguments.of(true, 2, 1),
        )

        @JvmStatic
        fun `Should updateUserPreferenceSelectedEventProfileById update default profile`(): Stream<Arguments> {
            val profileId = UUID.randomUUID()
            return Stream.of(
                Arguments.of(profileId, PreferencesModel(), 1, 1),
                Arguments.of(profileId, PreferencesModel(selectedProfile = EventProfileModel().apply { id = profileId }), 1, 0),
            )
        }
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findByUser return the User's Preferences`(
        isFirstEmpty: Boolean,
        expectedCallOnFindByUserId: Int,
        expectedCallOnSave: Int,
    ) {
        // Arrange
        val uuid = UUID.randomUUID()
        val currentUser = CurrentUserModel().apply { id = uuid }
        val preferences = Mono.just(PreferencesModel())
        `when`(repository.findByUserId(any(), any())).thenReturn(
            if (isFirstEmpty) Mono.empty() else preferences,
            preferences
        )
        lenient().`when`(repository.save(any())).thenReturn(preferences)

        // Act
        service.findByUser(currentUser).block()

        // Assert
        verify(repository, times(expectedCallOnFindByUserId)).findByUserId(uuid, onlyVisible = true)
        verify(repository, times(expectedCallOnSave)).save(any())
    }

    @ParameterizedTest
    @MethodSource
    fun `Should updateUserPreferenceSelectedEventProfileById update default profile`(
        profileId: UUID,
        currentPreferences: PreferencesModel,
        expectedCallOnFindByUserId: Int,
        expectedCallOnSave: Int,
    ) {
        // Arrange
        val uuid = UUID.randomUUID()
        val currentUser = CurrentUserModel().apply { id = uuid }
        val profile = EventProfileModel().apply { id = profileId }

        `when`(eventProfileService.findUserEventProfileById(any(), any(), any())).thenReturn(Mono.just(profile))
        `when`(repository.findByUserId(any(), any())).thenReturn(Mono.just(currentPreferences))
        lenient().`when`(repository.save(any())).thenReturn(Mono.just(currentPreferences))

        // Act
        service.updateUserPreferenceSelectedEventProfileById(currentUser, profileId).block()

        // Assert
        verify(eventProfileService, times(1)).findUserEventProfileById(currentUser, profileId, onlyVisible = true)
        verify(repository, times(expectedCallOnFindByUserId)).findByUserId(uuid, onlyVisible = true)
        verify(repository, times(expectedCallOnSave)).save(any())
    }

    @Test
    fun `Should updateUserPreferenceSelectedEventProfileById throw RegistryExceptionModel`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val profileId = UUID.randomUUID()
        val currentUser = CurrentUserModel().apply { id = uuid }
        `when`(eventProfileService.findUserEventProfileById(any(), any(), any())).thenReturn(Mono.empty())

        // Act
        val result = assertThrows(RegistryException::class.java) {
            service.updateUserPreferenceSelectedEventProfileById(currentUser, profileId).block()
        }

        // Assert
        assertEquals(NOT_FOUND, result.status)
        assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
        assertEquals(profileId.toString(), result.args?.get("identifier"))
    }
}
