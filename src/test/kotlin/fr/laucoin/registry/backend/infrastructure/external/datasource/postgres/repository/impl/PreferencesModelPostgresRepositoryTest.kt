package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferencesEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.PreferencesEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IPreferencesEntityRepository
import java.util.UUID
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import reactor.core.publisher.Mono

class PreferencesModelPostgresRepositoryTest {
    private val repository: IPreferencesEntityRepository = mock()
    private val mapper: PreferencesEntityMapper = spy()
    private val modelRepository: PreferenceModelPostgresRepository =
        PreferenceModelPostgresRepository(repository, mapper)

    @Test
    fun `Should findByUserId call repository findByUserId`() {
        // Arrange
        val preferences = PreferencesEntity()
        val userId = UUID.randomUUID()
        val onlyVisible = true
        `when`(repository.findByUserId(any(), any())).thenReturn(Mono.just(preferences))

        // Act
        modelRepository.findByUserId(userId, onlyVisible).block()

        // Assert
        verify(repository, times(1)).findByUserId(userId, onlyVisible)
        verify(mapper, times(1)).toModel(preferences)
    }

    @Test
    fun `Should save call repository save`() {
        // Arrange
        val preferences = PreferencesModel()
        val preferencesEntity = PreferencesEntity()
        `when`(repository.save(any())).thenReturn(Mono.just(preferencesEntity))

        // Act
        modelRepository.save(preferences).block()

        // Assert
        verify(repository, times(1)).save(any())
        verify(mapper, times(1)).toEntity(preferences)
        verify(mapper, times(1)).toModel(preferencesEntity)
    }
}
