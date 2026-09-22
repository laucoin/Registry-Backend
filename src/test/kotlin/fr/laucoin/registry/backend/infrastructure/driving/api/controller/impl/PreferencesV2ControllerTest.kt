package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PreferencesReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.PreferencesReaderDtoMapper
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.util.UUID
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

class PreferencesV2ControllerTest: TestContext() {
	@MockitoBean
	private lateinit var service: IPreferencesService

	@MockitoBean
	private lateinit var readerMapper: PreferencesReaderDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/users/preferences"
	}

	@Test
	fun `Should updateTheme return 200`() {
		// Arrange
		whenever(service.updateTheme(any(), any())).thenReturn(Mono.just(PreferencesModel()))
		whenever(readerMapper.toDto(any())).thenReturn(PreferencesReaderDto())

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/theme", emptyList(), listOf(Pair("theme", ThemeEnum.DARK))))
			.exchange()

		// Assert
		result.body<PreferencesReaderDto>(OK)
		verify(service).updateTheme(any(), eq(ThemeEnum.DARK))
	}

	@Test
	fun `Should updateLanguage return 200`() {
		// Arrange
		whenever(service.updateLanguage(any(), any())).thenReturn(Mono.just(PreferencesModel()))
		whenever(readerMapper.toDto(any())).thenReturn(PreferencesReaderDto())

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/language", emptyList(), listOf(Pair("language", "fr"))))
			.exchange()

		// Assert
		result.body<PreferencesReaderDto>(OK)
		verify(service).updateLanguage(any(), eq("fr"))
	}

	@Test
	fun `Should updateSelectedProjectProfile return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.updateUserPreferenceSelectedProjectProfileById(any(), anyOrNull())).thenReturn(Mono.just(PreferencesModel()))
		whenever(readerMapper.toDto(any())).thenReturn(PreferencesReaderDto())

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/profile/select", emptyList(), listOf(Pair("profileId", uuid))))
			.exchange()

		// Assert
		result.body<PreferencesReaderDto>(OK)
		verify(service).updateUserPreferenceSelectedProjectProfileById(any(), eq(uuid))
	}

	@Test
	fun `Should updateSelectedProjectProfileWithProjectId return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.updateUserPreferenceSelectedProjectProfileByProjectId(any(), any())).thenReturn(Mono.just(PreferencesModel()))
		whenever(readerMapper.toDto(any())).thenReturn(PreferencesReaderDto())

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/projects/{projectId}/profile/select", listOf(uuid), emptyList()))
			.exchange()

		// Assert
		result.body<PreferencesReaderDto>(OK)
		verify(service).updateUserPreferenceSelectedProjectProfileByProjectId(any(), eq(uuid))
	}
}
