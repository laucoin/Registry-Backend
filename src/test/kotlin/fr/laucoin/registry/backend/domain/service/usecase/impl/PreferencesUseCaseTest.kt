package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.enumeration.RefreshModeEnum
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.service.core.IPreferencesService
import fr.laucoin.registry.backend.domain.service.usecase.IPreferencesUseCase
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono

class PreferencesUseCaseTest {
	private val service: IPreferencesService = mock()
	private val useCase: IPreferencesUseCase = PreferencesUseCase(service)

	@Test
	fun `Should updateTheme delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val theme: ThemeEnum = ThemeEnum.entries.first()
		val expected: Mono<PreferencesModel> = Mono.empty()
		whenever(service.updateTheme(currentUser, theme)).thenReturn(expected)

		// Act
		val result = useCase.updateTheme(currentUser, theme)

		// Assert
		assertSame(expected, result)
		verify(service).updateTheme(currentUser, theme)
	}

	@Test
	fun `Should updateLanguage delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val language: String = "value"
		val expected: Mono<PreferencesModel> = Mono.empty()
		whenever(service.updateLanguage(currentUser, language)).thenReturn(expected)

		// Act
		val result = useCase.updateLanguage(currentUser, language)

		// Assert
		assertSame(expected, result)
		verify(service).updateLanguage(currentUser, language)
	}

	@Test
	fun `Should updateRefreshMode delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val refreshMode: RefreshModeEnum = RefreshModeEnum.entries.first()
		val expected: Mono<PreferencesModel> = Mono.empty()
		whenever(service.updateRefreshMode(currentUser, refreshMode)).thenReturn(expected)

		// Act
		val result = useCase.updateRefreshMode(currentUser, refreshMode)

		// Assert
		assertSame(expected, result)
		verify(service).updateRefreshMode(currentUser, refreshMode)
	}
}
