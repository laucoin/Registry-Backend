package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ProjectProfileReaderDto
import java.util.Locale
import java.util.stream.Stream
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PreferenceReaderDtoMapperTest {
    private val profileMapper: ProjectProfileReaderDtoMapper = mock()
    private val mapper: PreferenceReaderDtoMapper = PreferenceReaderDtoMapper(profileMapper)

    companion object {
        @JvmStatic
        fun `Should toDto convert PreferencesModel to PreferenceReaderDto`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    PreferencesModel(),
                    0,
                ),
                Arguments.of(
                    PreferencesModel().apply {
                        selectedProfile = ProjectProfileModel()
                    },
                    1,
                ),
            )
        }
    }

    @ParameterizedTest
    @MethodSource
    fun `Should toDto convert PreferencesModel to PreferenceReaderDto`(
        preferences: PreferencesModel,
        expectedProfileCast: Int,
    ) {
        // Arrange
        whenever(profileMapper.toDto(any(), any())).thenReturn(ProjectProfileReaderDto())

        // Act
        mapper.toDto(preferences, Locale.getDefault())

        // Assert
        verify(profileMapper, times(expectedProfileCast)).toDto(
            preferences.selectedProfile ?: ProjectProfileModel(),
            Locale.getDefault(),
        )
    }
}
