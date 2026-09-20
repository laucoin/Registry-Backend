package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum.DARK
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum.LIGHT
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum.SYSTEM
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.port.IPreferencesPort
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono

class PreferencesServiceTest {
	private val port: IPreferencesPort = mock()
	private val supportedLocale: List<String> = listOf(EN_US, FR_FR)
	private val service: IPreferencesService = PreferencesService(port, supportedLocale)

	private companion object {
		private const val EN_US = "en-US"
		private const val FR_FR = "fr-FR"

		@JvmStatic
		fun `Should findByUser return the User's Preferences`(): Stream<Arguments> = Stream.of(
			Arguments.of(false, 1, 0),
			Arguments.of(true, 2, 1),
		)

		@JvmStatic
		fun `Should updateTheme update theme if necessary`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(LIGHT, DARK, 1),
				Arguments.of(DARK, DARK, 0),
				Arguments.of(SYSTEM, LIGHT, 1),
				Arguments.of(LIGHT, LIGHT, 0),
			)
		}

		@JvmStatic
		fun `Should updateLanguage update theme if necessary`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(EN_US, FR_FR, 1),
				Arguments.of(FR_FR, FR_FR, 0),
				Arguments.of(FR_FR, EN_US, 1),
				Arguments.of(EN_US, EN_US, 0),
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
		val preferences = Mono.just(PreferencesModel())

		whenever(port.findByUserId(any(), anyOrNull()))
			.thenReturn(if (isFirstEmpty) Mono.empty() else preferences, preferences)

		whenever(port.save(any())).thenReturn(preferences)

		// Act
		service.findByUser(currentUser()).block()

		// Assert
		verify(port, times(expectedCallOnFindByUserId)).findByUserId(currentUser().id!!, visibilitySearched = null)
		verify(port, times(expectedCallOnSave)).save(any())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should updateTheme update theme if necessary`(
		theme: ThemeEnum,
		newTheme: ThemeEnum,
		expectedCallOnUpdateTheme: Int,
	) {
		// Arrange
		val uuid = UUID.randomUUID()
		val currentUser = CurrentUserModel().apply { id = uuid }
		val preference = PreferencesModel().apply { this.theme = theme }

		whenever(port.findByUserId(any(), anyOrNull())).thenReturn(Mono.just(preference))
		whenever(port.save(any())).thenReturn(Mono.just(preference))

		// Act
		service.updateTheme(currentUser, newTheme).block()

		// Assert
		verify(port).findByUserId(uuid, visibilitySearched = null)
		verify(port, times(expectedCallOnUpdateTheme)).save(any())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should updateLanguage update theme if necessary`(
		language: String,
		newLanguage: String,
		expectedCallOnUpdateLanguage: Int,
	) {
		// Arrange
		val uuid = UUID.randomUUID()
		val currentUser = CurrentUserModel().apply { id = uuid }
		val preference = PreferencesModel().apply { this.language = language }

		whenever(port.findByUserId(any(), anyOrNull())).thenReturn(Mono.just(preference))
		whenever(port.save(any())).thenReturn(Mono.just(preference))

		// Act
		service.updateLanguage(currentUser, newLanguage).block()

		// Assert
		verify(port).findByUserId(uuid, visibilitySearched = null)
		verify(port, times(expectedCallOnUpdateLanguage)).save(any())
	}
}
