package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum.DARK
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum.LIGHT
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum.SYSTEM
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IPreferencesPort
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import fr.laucoin.registry.backend.test.ModelExt.commonProjectProfile
import fr.laucoin.registry.backend.test.ModelExt.projectProfileId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.NOT_FOUND
import reactor.core.Exceptions
import reactor.core.publisher.Mono
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals

class PreferencesServiceTest {
	private val port: IPreferencesPort = mock()
	private val projectProfilePort: IProjectProfilePort = mock()
	private val supportedLocale: List<String> = listOf(EN_US, FR_FR)
	private val service: IPreferencesService = PreferencesService(port, projectProfilePort, supportedLocale)

	private companion object {
		private const val EN_US = "en-US"
		private const val FR_FR = "fr-FR"

		@JvmStatic
		fun `Should findByUser return the User's Preferences`(): Stream<Arguments> = Stream.of(
			Arguments.of(false, 1, 0),
			Arguments.of(true, 2, 1),
		)

		@JvmStatic
		fun `Should updateUserPreferenceSelectedProjectProfileById update default profile`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(projectProfileId, PreferencesModel(), 1, 1),
				Arguments.of(projectProfileId, PreferencesModel(selectedProfile = commonProjectProfile()), 1, 0),
				Arguments.of(null, PreferencesModel(selectedProfile = commonProjectProfile()), 0, 1),
			)
		}

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
	fun `Should updateUserPreferenceSelectedProjectProfileById update default profile`(
		profileId: UUID?,
		currentPreferences: PreferencesModel,
		expectedCallOnFindProjectProfileByUserIdAndId: Int,
		expectedCallOnSave: Int,
	) {
		// Arrange
		val uuid = UUID.randomUUID()
		val currentUser = CurrentUserModel().apply { id = uuid }
		val profile = ProjectProfileModel().apply { id = profileId }

		whenever(
			projectProfilePort.findProjectProfileByUserIdAndId(
				any(),
				any(),
				anyOrNull()
			)
		).thenReturn(Mono.just(profile))
		whenever(port.findByUserId(any(), anyOrNull())).thenReturn(Mono.just(currentPreferences))
		whenever(port.save(any())).thenReturn(Mono.just(currentPreferences))

		// Act
		service.updateUserPreferenceSelectedProjectProfileById(currentUser, profileId).block()

		// Assert
		verify(
			projectProfilePort,
			times(expectedCallOnFindProjectProfileByUserIdAndId)
		).findProjectProfileByUserIdAndId(
			eq(currentUser.id!!), any(), eq(true)
		)
		verify(port).findByUserId(uuid, visibilitySearched = null)
		verify(port, times(expectedCallOnSave)).save(any())
	}

	@Test
	fun `Should updateUserPreferenceSelectedProjectProfileByProjectId update default profile`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val currentUser = CurrentUserModel().apply { id = uuid }
		val profile = ProjectProfileModel().apply { id = uuid }
		val currentPreferences = PreferencesModel().apply { selectedProfile = profile }
		val search = ProjectProfileSearchParamModel(
			visibilitySearched = true,
			availabilitySearched = true,
			statusSearched = listOf(ACCEPTED)
		)

		whenever(
			projectProfilePort.findProjectProfileByProjectAndUserId(
				any(),
				any(),
				anyOrNull()
			)
		).thenReturn(Mono.just(profile))
		whenever(port.findByUserId(any(), anyOrNull())).thenReturn(Mono.just(currentPreferences))

		// Act
		service.updateUserPreferenceSelectedProjectProfileByProjectId(currentUser, uuid).block()

		// Assert
		verify(projectProfilePort).findProjectProfileByProjectAndUserId(
			eq(uuid), eq(currentUser.id!!), eq(search)
		)
		verify(port).findByUserId(uuid, visibilitySearched = null)
		verify(port, never()).save(any())
	}

	@Test
	fun `Should updateUserPreferenceSelectedProjectProfileById throw RegistryException`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val profileId = UUID.randomUUID()
		val currentUser = CurrentUserModel().apply { id = uuid }
		whenever(
			projectProfilePort.findProjectProfileByUserIdAndId(
				any(),
				any(),
				anyOrNull()
			)
		).thenReturn(Mono.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.updateUserPreferenceSelectedProjectProfileById(currentUser, profileId).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
		assertEquals(1, result.args?.size)
		assertEquals(profileId.toString(), result.args?.first())

		verify(projectProfilePort).findProjectProfileByUserIdAndId(
			currentUser.id!!,
			profileId,
			visibilitySearched = true
		)
		verify(port, never()).findByUserId(any(), anyOrNull())
		verify(port, never()).save(any())
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
