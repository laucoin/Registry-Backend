package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum.DARK
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class PreferenceReaderDtoMapperTest {
	private val mapper = PreferenceReaderDtoMapper()

	@Test
	fun `Should toDto convert PreferencesModel to PreferenceReaderDto`() {
		// Arrange
		val preferences = PreferencesModel().apply {
			theme = DARK
			language = "fr-FR"
		}

		// Act
		val result = mapper.toDto(preferences)

		// Assert
		assertEquals(DARK, result.theme)
		assertEquals("fr-FR", result.language)
	}
}
