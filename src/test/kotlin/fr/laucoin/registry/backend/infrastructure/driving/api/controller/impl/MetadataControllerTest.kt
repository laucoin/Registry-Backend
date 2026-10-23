package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.COMMUNICATION
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectOptionsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ProjectOptionsReaderDtoMapper
import reactor.core.publisher.Flux
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.AvailabilityStatusReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementTypeReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.PresenceStatusReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ProjectProfileStatusReaderDtoMapper
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient

class MetadataControllerTest: TestContext() {
	@MockitoBean
	private lateinit var movementTypeReaderMapper: MovementTypeReaderDtoMapper

	@MockitoBean
	private lateinit var presenceStatusMapper: PresenceStatusReaderDtoMapper

	@MockitoBean
	private lateinit var availabilityStatusMapper: AvailabilityStatusReaderDtoMapper

	@MockitoBean
	private lateinit var profileStatusReaderMapper: ProjectProfileStatusReaderDtoMapper

	@MockitoBean
	private lateinit var projectService: IProjectService

	@MockitoBean
	private lateinit var projectOptionsReaderMapper: ProjectOptionsReaderDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/metadata"
	}

	@Test
	fun `Should getProjectProfileStatus return 200`() {
		// Arrange
		whenever(profileStatusReaderMapper.toDto(any())).thenReturn(LabelDto("value", "label"))

		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/profiles/status", emptyList(), emptyList()))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verifyNoInteractions(movementTypeReaderMapper)
		verify(profileStatusReaderMapper, atLeastOnce()).toDto(any())
		verifyNoInteractions(presenceStatusMapper)
	}

	@Test
	fun `Should getPresenceStatus return 200`() {
		// Arrange
		whenever(presenceStatusMapper.toDto(any())).thenReturn(LabelDto("value", "label"))

		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/presences/status", emptyList(), emptyList()))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verifyNoInteractions(movementTypeReaderMapper)
		verifyNoInteractions(profileStatusReaderMapper)
		verify(presenceStatusMapper, atLeastOnce()).toDto(any())
	}

	@Test
	fun `Should getAvailabilitiesStatus return 200`() {
		// Arrange
		whenever(availabilityStatusMapper.toDto(any())).thenReturn(LabelDto("value", "label"))

		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/availabilities/status", emptyList(), emptyList()))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verifyNoInteractions(movementTypeReaderMapper)
		verifyNoInteractions(profileStatusReaderMapper)
		verify(availabilityStatusMapper, atLeastOnce()).toDto(any())
	}

	@Test
	fun `Should getMovementsTypes return 200`() {
		// Arrange
		whenever(movementTypeReaderMapper.toDto(any())).thenReturn(LabelDto("value", "label"))

		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/movements/types", emptyList(), emptyList()))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(movementTypeReaderMapper, atLeastOnce()).toDto(any())
		verifyNoInteractions(profileStatusReaderMapper)
		verifyNoInteractions(presenceStatusMapper)
	}

	@Test
	fun `Should getAvailableProjectOptions return 200`() {
		// Arrange
		whenever(projectService.availableProjectOptions()).thenReturn(Flux.just(COMMUNICATION))
		whenever(projectOptionsReaderMapper.toDto(any())).thenReturn(
			ProjectOptionsReaderDto(ACTIVITY, "label", "question", emptyList())
		)

		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/projects/options", emptyList(), emptyList()))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(projectService).availableProjectOptions()
		verify(projectOptionsReaderMapper).toDto(any())
	}

	@Test
	fun `Should metadata be reachable without authentication`() {
		// Arrange
		whenever(movementTypeReaderMapper.toDto(any())).thenReturn(LabelDto("value", "label"))

		// Act
		val result = webClient
			.get()
			.uri(uriBuilder("$BASE_URL/movements/types", emptyList(), emptyList()))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
	}

	@Test
	fun `Should the other endpoints still require authentication`() {
		// Act
		val result = webClient
			.get()
			.uri(uriBuilder("/api/v2/projects", emptyList(), emptyList()))
			.exchange()

		// Assert
		result.expectStatus().isUnauthorized
	}

	@Test
	fun `Should the removed v1 metadata api not exist anymore`() {
		// Act
		val result = webClient
			.get()
			.uri(uriBuilder("/api/v1/metadata/movements/types", emptyList(), emptyList()))
			.exchange()

		// Assert
		result.expectStatus().isNotFound
	}
}
