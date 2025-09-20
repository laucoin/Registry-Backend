package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.AvailabilityStatusReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementTypeReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.PresenceStatusReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.ProjectProfileStatusReaderDtoMapper
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

	@Autowired
	private lateinit var webClient: WebTestClient

	companion object {
		private const val BASE_URL = "/api/metadata"
	}

	@Test
	fun `Should getProjectProfileStatus return 200`() {
		// Arrange
		whenever(profileStatusReaderMapper.toDto(any(), any())).thenReturn(LabelDto("value", "label"))

		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/profiles/status", emptyList(), emptyList()))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verifyNoInteractions(movementTypeReaderMapper)
		verify(profileStatusReaderMapper, atLeastOnce()).toDto(any(), any())
		verifyNoInteractions(presenceStatusMapper)
	}

	@Test
	fun `Should getPresenceStatus return 200`() {
		// Arrange
		whenever(presenceStatusMapper.toDto(any(), any())).thenReturn(LabelDto("value", "label"))

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
		verify(presenceStatusMapper, atLeastOnce()).toDto(any(), any())
	}

	@Test
	fun `Should getAvailabilitiesStatus return 200`() {
		// Arrange
		whenever(availabilityStatusMapper.toDto(any(), any())).thenReturn(LabelDto("value", "label"))

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
		verify(availabilityStatusMapper, atLeastOnce()).toDto(any(), any())
	}

	@Test
	fun `Should getMovementsTypes return 200`() {
		// Arrange
		whenever(movementTypeReaderMapper.toDto(any(), any())).thenReturn(LabelDto("value", "label"))

		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/movements/types", emptyList(), emptyList()))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(movementTypeReaderMapper, atLeastOnce()).toDto(any(), any())
		verifyNoInteractions(profileStatusReaderMapper)
		verifyNoInteractions(presenceStatusMapper)
	}
}
