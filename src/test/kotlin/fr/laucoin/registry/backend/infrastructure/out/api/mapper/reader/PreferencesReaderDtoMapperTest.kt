package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum.DARK
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class PreferencesReaderDtoMapperTest {
	private val mapper: PreferencesReaderDtoMapper = PreferencesReaderDtoMapper()

	@Test
	fun `Should toDto convert PreferencesModel to PreferencesReaderDto`() {
		// Arrange
		val model = PreferencesModel(
			userId = UUID.randomUUID(),
			theme = DARK,
			language = "fr",
			selectedProfile = ProjectProfileModel(),
		).apply {
			id = UUID.randomUUID()
			visible = true
			creation = HistoryModel()
			lastEdition = HistoryModel()
		}

		// Act
		val result = mapper.toDto(model)

		// Assert
		assertEquals(model.id, result.id)
		assertEquals(model.visible, result.visible)
		assertEquals(model.creation, result.creation)
		assertEquals(model.lastEdition, result.lastEdition)
		assertEquals(model.userId, result.userId)
		assertEquals(model.theme, result.theme)
		assertEquals(model.language, result.language)
		assertEquals(model.selectedProfile, result.selectedProfile)
	}
}
