package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TYPE_MISMATCH
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum.DARK
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.PreferencesLanguageWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.PreferencesSelectProfileWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.PreferencesThemeWriterDto
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import java.util.UUID

class PreferencesV2ControllerTest : TestContext() {
	@MockitoBean
	private lateinit var service: IPreferencesService

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/users/preferences"
	}

	@Test
	fun `Should updateTheme act as an explicit POST action with the value in the body`() {
		// Arrange
		whenever(service.updateTheme(any(), any())).thenReturn(Mono.just(PreferencesModel()))

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/theme", emptyList(), emptyList()))
			.bodyValue(PreferencesThemeWriterDto(theme = DARK))
			.exchange()

		// Assert
		result.body<PreferencesModel>(OK)
		verify(service).updateTheme(any(), eq(DARK))
	}

	@Test
	fun `Should updateTheme reject a missing theme with 400`() {
		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/theme", emptyList(), emptyList()))
			.bodyValue(PreferencesThemeWriterDto())
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PARAMETER_TYPE_MISMATCH)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should updateLanguage act as an explicit POST action with the value in the body`() {
		// Arrange
		whenever(service.updateLanguage(any(), any())).thenReturn(Mono.just(PreferencesModel()))

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/language", emptyList(), emptyList()))
			.bodyValue(PreferencesLanguageWriterDto(language = "fr"))
			.exchange()

		// Assert
		result.body<PreferencesModel>(OK)
		verify(service).updateLanguage(any(), eq("fr"))
	}

	@Test
	fun `Should updateLanguage reject a blank language with 400`() {
		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/language", emptyList(), emptyList()))
			.bodyValue(PreferencesLanguageWriterDto(language = ""))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PARAMETER_TYPE_MISMATCH)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should selectProfile select by profile id`() {
		// Arrange
		val profileId = UUID.randomUUID()
		whenever(service.updateUserPreferenceSelectedProjectProfileById(any(), anyOrNull()))
			.thenReturn(Mono.just(PreferencesModel()))

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/select-profile", emptyList(), emptyList()))
			.bodyValue(PreferencesSelectProfileWriterDto(profileId = profileId))
			.exchange()

		// Assert
		result.body<PreferencesModel>(OK)
		verify(service).updateUserPreferenceSelectedProjectProfileById(any(), eq(profileId))
	}

	@Test
	fun `Should selectProfile select by project id`() {
		// Arrange
		val projectId = UUID.randomUUID()
		whenever(service.updateUserPreferenceSelectedProjectProfileByProjectId(any(), any()))
			.thenReturn(Mono.just(PreferencesModel()))

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/select-profile", emptyList(), emptyList()))
			.bodyValue(PreferencesSelectProfileWriterDto(projectId = projectId))
			.exchange()

		// Assert
		result.body<PreferencesModel>(OK)
		verify(service).updateUserPreferenceSelectedProjectProfileByProjectId(any(), eq(projectId))
	}

	@Test
	fun `Should selectProfile clear the selection with an empty body`() {
		// Arrange
		whenever(service.updateUserPreferenceSelectedProjectProfileById(any(), anyOrNull()))
			.thenReturn(Mono.just(PreferencesModel()))

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/select-profile", emptyList(), emptyList()))
			.bodyValue(PreferencesSelectProfileWriterDto())
			.exchange()

		// Assert
		result.body<PreferencesModel>(OK)
		verify(service).updateUserPreferenceSelectedProjectProfileById(any(), anyOrNull())
	}

	@Test
	fun `Should selectProfile reject both identifiers with 400`() {
		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/select-profile", emptyList(), emptyList()))
			.bodyValue(PreferencesSelectProfileWriterDto(profileId = UUID.randomUUID(), projectId = UUID.randomUUID()))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PARAMETER_TYPE_MISMATCH)
		verifyNoInteractions(service)
	}
}
