package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.USER_ROLE_PREFIX
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_PROJECT_R
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.PreferenceReaderDto
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.authority.SimpleGrantedAuthority

class CurrentUserReaderDtoMapperTest {
	private val translateService: ITranslateService = mock()
	private val preferenceMapper: PreferenceReaderDtoMapper = mock()
	private val mapper: CurrentUserReaderDtoMapper = CurrentUserReaderDtoMapper(translateService, preferenceMapper)

	companion object {
		@JvmStatic
		fun `Should toDto convert CurrentUserModel to CurrentUserReaderDto`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(
					CurrentUserModel(
						authorities = mutableListOf(SimpleGrantedAuthority(REGISTRY_PROJECT_R)),
						preferences = PreferencesModel(),
					).apply {
						id = UUID.randomUUID()
						oidcId = UUID.randomUUID()
						firstName = "John"
						lastName = "DOE"
						email = "john.doe@test.com"
						role = "ROLE"
						birthday = LocalDate.now()
						lastLogin = ZonedDateTime.now()
						purged = false
						visible = true
						creation = HistoryModel()
						lastEdition = HistoryModel()
					},
					1,
					1,
				),
				Arguments.of(
					CurrentUserModel(
						authorities = mutableListOf(SimpleGrantedAuthority(REGISTRY_PROJECT_R)),
					).apply {
						id = UUID.randomUUID()
						oidcId = UUID.randomUUID()
						firstName = "John"
						lastName = "DOE"
						email = "john.doe@test.com"
						birthday = LocalDate.now()
						lastLogin = ZonedDateTime.now()
						purged = false
						visible = true
						creation = HistoryModel()
						lastEdition = HistoryModel()
					},
					0,
					0,
				),
			)
		}
	}

	@ParameterizedTest
	@MethodSource
	fun `Should toDto convert CurrentUserModel to CurrentUserReaderDto`(
		currentUser: CurrentUserModel,
		expectedRoleTranslation: Int,
		expectedPreferencesCast: Int,
	) {
		// Arrange
		val authority = REGISTRY_PROJECT_R
		whenever(translateService.getMessage(any(), any(), anyOrNull(), anyOrNull())).thenReturn("Role translated")
		whenever(preferenceMapper.toDto(any(), any())).thenReturn(PreferenceReaderDto())

		// Act
		val result = mapper.toDto(currentUser, Locale.getDefault())

		// Assert
		verify(translateService, times(expectedRoleTranslation)).getMessage(
			"${USER_ROLE_PREFIX}ROLE",
			Locale.getDefault()
		)
		verify(preferenceMapper, times(expectedPreferencesCast)).toDto(
			currentUser.preferences ?: PreferencesModel(),
			Locale.getDefault()
		)

		assertEquals(currentUser.id, result.id)
		assertEquals(1, result.authorities.size)
		assertEquals(authority, result.authorities.first())
		assertEquals(currentUser.firstName, result.firstName)
		assertEquals(currentUser.lastName, result.lastName)
		assertEquals(currentUser.email, result.email)
		assertEquals(currentUser.role, result.role?.value)
		assertEquals(currentUser.birthday, result.birthday)
		assertEquals(currentUser.lastLogin, result.lastLogin)
		assertEquals(currentUser.purged, result.purged)
		assertEquals(currentUser.visible, result.visible)
		assertEquals(currentUser.creation, result.creation)
		assertEquals(currentUser.lastEdition, result.lastEdition)
	}
}
