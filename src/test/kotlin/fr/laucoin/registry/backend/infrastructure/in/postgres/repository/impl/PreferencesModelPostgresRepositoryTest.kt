package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.port.IPreferencesPort
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.PreferencesEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.IPreferencesEntityRepository
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import kotlin.test.assertNotNull

class PreferencesModelPostgresRepositoryTest : TestContext() {
	@MockitoSpyBean
	private lateinit var postgresRepository: IPreferencesEntityRepository

	@MockitoSpyBean
	private lateinit var mapper: PreferencesEntityMapper

	@Autowired
	private lateinit var repository: IPreferencesPort

	@Test
	fun `Should findByUserId call repository findByUserId`() {
		// Act
		val result = repository.findByUserId(currentUser().id!!, visibilitySearched = null).block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository).findByUserId(
			currentUser().id!!,
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
