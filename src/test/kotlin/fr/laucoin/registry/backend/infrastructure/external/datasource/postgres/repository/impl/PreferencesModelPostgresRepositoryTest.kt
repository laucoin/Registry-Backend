package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.repository.IPreferencesModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.PreferencesEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IPreferencesEntityRepository
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

class PreferencesModelPostgresRepositoryTest(
    @Autowired private val repository: IPreferencesModelRepository
): TestContext() {
    @MockitoSpyBean
    private lateinit var postgresRepository: IPreferencesEntityRepository

    @MockitoSpyBean
    private lateinit var mapper: PreferencesEntityMapper

    @Test
    fun `Should findByUserId call repository findByUserId`() {
        // Act
        val result = repository.findByUserId(currentUser().id !!, visibilitySearched = null).block()

        // Assert
        assertNotNull(result)
        verify(postgresRepository).findByUserId(
            currentUser().id !!,
            visibilitySearched = null,
        )
        verify(mapper).toModel(any())
    }

    @Test
    fun `Should save call repository save`() {
        // Arrange
        val preferences = PreferencesModel().apply {
            userId = currentUser().id
        }

        // Act
        val result = repository.save(preferences).block()

        // Assert
        assertNotNull(result?.id)
        verify(postgresRepository).save(any())
        verify(mapper).toEntity(any())
        verify(mapper).toModel(any())
    }
}
