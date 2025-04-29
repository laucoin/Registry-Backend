package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.repository.IProjectProfileModelRepository
import fr.laucoin.registry.backend.domain.repository.IPreferencesModelRepository
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.NOT_FOUND
import reactor.core.Exceptions
import reactor.core.publisher.Mono

class PreferencesServiceTest {
    private val repository: IPreferencesModelRepository = mock()
    private val projectProfileRepository: IProjectProfileModelRepository = mock()
    private val service: IPreferencesService = PreferencesService(repository, projectProfileRepository)

    companion object {
        @JvmStatic
        fun `Should findByUser return the User's Preferences`(): Stream<Arguments> = Stream.of(
            Arguments.of(false, 1, 0),
            Arguments.of(true, 2, 1),
        )

        @JvmStatic
        fun `Should updateUserPreferenceSelectedProjectProfileById update default profile`(): Stream<Arguments> {
            val profileId = UUID.randomUUID()
            return Stream.of(
                Arguments.of(profileId, PreferencesModel(), 1, 1),
                Arguments.of(profileId, PreferencesModel(selectedProfile = ProjectProfileModel().apply { id = profileId }), 1, 0),
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
        whenever(repository.findByUserId(any(), anyOrNull())).thenReturn(
            if (isFirstEmpty) Mono.empty() else preferences,
            preferences
        )
        whenever(repository.save(any())).thenReturn(preferences)

        // Act
        service.findByUser(currentUser).block()

        // Assert
        verify(repository, times(expectedCallOnFindByUserId)).findByUserId(uuid, visibilitySearched = null)
        verify(repository, times(expectedCallOnSave)).save(any())
    }

    @ParameterizedTest
    @MethodSource
    fun `Should updateUserPreferenceSelectedProjectProfileById update default profile`(
        profileId: UUID,
        currentPreferences: PreferencesModel,
        expectedCallOnFindByUserId: Int,
        expectedCallOnSave: Int,
    ) {
        // Arrange
        val uuid = UUID.randomUUID()
        val currentUser = CurrentUserModel().apply { id = uuid }
        val profile = ProjectProfileModel().apply { id = profileId }

        whenever(projectProfileRepository.findProjectProfileByUserIdAndId(any(), any(), anyOrNull())).thenReturn(Mono.just(profile))
        whenever(repository.findByUserId(any(), anyOrNull())).thenReturn(Mono.just(currentPreferences))
        whenever(repository.save(any())).thenReturn(Mono.just(currentPreferences))

        // Act
        service.updateUserPreferenceSelectedProjectProfileById(currentUser, profileId).block()

        // Assert
        verify(projectProfileRepository).findProjectProfileByUserIdAndId(currentUser.id !!, profileId, visibilitySearched = true)
        verify(repository, times(expectedCallOnFindByUserId)).findByUserId(uuid, visibilitySearched = null)
        verify(repository, times(expectedCallOnSave)).save(any())
    }

    @Test
    fun `Should updateUserPreferenceSelectedProjectProfileById throw RegistryException`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val profileId = UUID.randomUUID()
        val currentUser = CurrentUserModel().apply { id = uuid }
        whenever(projectProfileRepository.findProjectProfileByUserIdAndId(any(), any(), anyOrNull())).thenReturn(Mono.empty())

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.updateUserPreferenceSelectedProjectProfileById(currentUser, profileId).block()
        }) as RegistryException

        // Assert
        verify(projectProfileRepository).findProjectProfileByUserIdAndId(currentUser.id !!, profileId, visibilitySearched = true)
        verify(repository, never()).findByUserId(any(), anyOrNull())
        verify(repository, never()).save(any())
        assertEquals(NOT_FOUND, result.status)
        assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
        assertEquals(profileId.toString(), result.args?.first())
    }
}
