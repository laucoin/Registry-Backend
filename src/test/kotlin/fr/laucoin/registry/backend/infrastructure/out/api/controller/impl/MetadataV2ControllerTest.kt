package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.FeaturesReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.AlertStatusReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.AvailabilityStatusReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementTypeReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.ParticipantTypeReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.PresenceStatusReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.ProjectProfileStatusReaderDtoMapper
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient

class MetadataV2ControllerTest : TestContext() {
	@MockitoBean
	private lateinit var movementTypeReaderMapper: MovementTypeReaderDtoMapper

	@MockitoBean
	private lateinit var presenceStatusMapper: PresenceStatusReaderDtoMapper

	@MockitoBean
	private lateinit var availabilityStatusMapper: AvailabilityStatusReaderDtoMapper

	@MockitoBean
	private lateinit var profileStatusReaderMapper: ProjectProfileStatusReaderDtoMapper

	@MockitoBean
	private lateinit var participantTypeReaderMapper: ParticipantTypeReaderDtoMapper

	@MockitoBean
	private lateinit var alertStatusReaderMapper: AlertStatusReaderDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/metadata"
	}

	@Test
	fun `Should getPresencesStatus return 200`() {
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
		verify(availabilityStatusMapper, atLeastOnce()).toDto(any())
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
		verify(profileStatusReaderMapper, atLeastOnce()).toDto(any())
	}

	@Test
	fun `Should getMovementTypes return 200`() {
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
	}

	@Test
	fun `Should getParticipantTypes return 200`() {
		// Arrange
		whenever(participantTypeReaderMapper.toDto(any())).thenReturn(LabelDto("value", "label"))

		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/participants/types", emptyList(), emptyList()))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(participantTypeReaderMapper, atLeastOnce()).toDto(any())
	}

	@Test
	fun `Should getFeatures return 200 with the light user switch`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/features", emptyList(), emptyList()))
			.exchange()

		// Assert
		val features = result.body<FeaturesReaderDto>(OK)
		assertEquals(true, features?.lightUser)
	}

	@Test
	fun `Should getAlertStatus return 200`() {
		// Arrange
		whenever(alertStatusReaderMapper.toDto(any())).thenReturn(LabelDto("value", "label"))

		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/alerts/status", emptyList(), emptyList()))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(alertStatusReaderMapper, atLeastOnce()).toDto(any())
	}
}
