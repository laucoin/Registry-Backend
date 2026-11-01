package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_DATETIME_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MESSAGE_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MOVEMENT_OR_ALERT_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.RESOURCE_MODIFIED_SINCE_READ
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_U
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ALERT
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_COMMUNICATION
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.service.ICommunicationService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.CommunicationWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.CommunicationReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.CommunicationWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.defaultMovementId
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.hiddenElementsAuthorities
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.PRECONDITION_FAILED
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID
import java.util.stream.Stream

class CommunicationControllerTest : TestContext() {
	@MockitoBean
	private lateinit var service: ICommunicationService

	@MockitoBean
	private lateinit var readerMapper: CommunicationReaderDtoMapper

	@MockitoBean
	private lateinit var writerMapper: CommunicationWriterDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/projects/{projectId}/communications"

		@JvmStatic
		fun `Wrong CommunicationDto`(): Stream<Arguments> = Stream.of(
			Arguments.of(
				CommunicationWriterDto(
					dateTime = null,
					alertId = null,
					movementId = defaultMovementId,
				),
				COMMUNICATION_DATETIME_NULL
			),
			Arguments.of(
				CommunicationWriterDto(
					dateTime = ZonedDateTime.now(ZoneOffset.UTC),
					alertId = null,
					movementId = null,
				),
				COMMUNICATION_MOVEMENT_OR_ALERT_NULL
			),
			Arguments.of(
				CommunicationWriterDto(
					dateTime = ZonedDateTime.now(ZoneOffset.UTC),
					message = "Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message.",
					alertId = null,
					movementId = defaultMovementId,
				),
				COMMUNICATION_MESSAGE_TOO_LONG
			),
		)
	}

	@Test
	fun `Should findCommunicationById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.findCommunicationById(any(), any(), anyOrNull())).thenReturn(Mono.just(CommunicationModel()))
		whenever(readerMapper.toDto(any())).thenReturn(CommunicationReaderDto())

		// Act
		val result = webClient
			.authenticate(*hiddenElementsAuthorities(), 
				buildAuthority(REGISTRY_PROJECT_COMMUNICATION_R),
				buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION)
			)
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<CommunicationReaderDto>(OK)

		verify(service).findCommunicationById(defaultProjectId, uuid, isVisible = null)
		verify(readerMapper).toDto(any())
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should findCommunicationById force the visible filter when the user cannot read hidden elements`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.findCommunicationById(any(), any(), anyOrNull())).thenReturn(Mono.just(CommunicationModel()))
		whenever(readerMapper.toDto(any())).thenReturn(CommunicationReaderDto())

		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_COMMUNICATION_R),
				buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION)
			)
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<CommunicationReaderDto>(OK)

		verify(service).findCommunicationById(defaultProjectId, uuid, isVisible = true)
		verify(readerMapper).toDto(any())
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should createCommunication return 201`() {
		// Arrange
		val createdId = UUID.randomUUID()
		val communication =
			CommunicationWriterDto(dateTime = ZonedDateTime.now(ZoneOffset.UTC), alertId = null, movementId = defaultMovementId)

		whenever(service.createCommunication(any(), any())).thenReturn(Mono.just(CommunicationModel()))
		whenever(readerMapper.toDto(any())).thenReturn(CommunicationReaderDto().apply { id = createdId })
		whenever(writerMapper.toModel(any(), any())).thenReturn(CommunicationModel())

		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_COMMUNICATION_C),
				buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION)
			)
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(communication)
			.exchange()

		// Assert
		result.expectHeader()
			.valueEquals("Location", "${BASE_URL.replace("{projectId}", defaultProjectId.toString())}/$createdId")
		result.body<CommunicationReaderDto>(CREATED)

		verify(service).createCommunication(any(), any())
		verify(readerMapper).toDto(any())
		verify(writerMapper).toModel(any(), eq(defaultProjectId))
	}

	@ParameterizedTest
	@MethodSource("Wrong CommunicationDto")
	fun `Should createCommunication return 400`(
		communication: CommunicationWriterDto,
		expectedCode: String,
	) {
		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(communication)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedCode)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(service)
	}


	@Test
	fun `Should updateCommunication return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val communication =
			CommunicationWriterDto(dateTime = ZonedDateTime.now(ZoneOffset.UTC), alertId = null, movementId = defaultMovementId)

		whenever(
			service.updateCommunicationById(
				any(),
				any(),
				any(),
				any()
			)
		).thenReturn(Mono.just(CommunicationModel()))
		whenever(readerMapper.toDto(any())).thenReturn(CommunicationReaderDto())
		whenever(writerMapper.toModel(any(), any())).thenReturn(CommunicationModel())

		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_COMMUNICATION_U),
				buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION)
			)
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.bodyValue(communication)
			.exchange()

		// Assert
		result.body<CommunicationReaderDto>(OK)

		verify(service).updateCommunicationById(any(), eq(defaultProjectId), eq(uuid), any())
		verify(readerMapper).toDto(any())
		verify(writerMapper).toModel(any(), eq(defaultProjectId))
	}

	@Test
	fun `Should updateCommunication accept the matching If-Match`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val communication =
			CommunicationWriterDto(dateTime = ZonedDateTime.now(ZoneOffset.UTC), alertId = null, movementId = defaultMovementId)

		whenever(
			service.updateCommunicationById(
				any(),
				any(),
				any(),
				any()
			)
		).thenReturn(Mono.just(CommunicationModel()))
		whenever(readerMapper.toDto(any())).thenReturn(CommunicationReaderDto())
		whenever(writerMapper.toModel(any(), any())).thenReturn(CommunicationModel())
		whenever(service.findCommunicationById(any(), any(), anyOrNull())).thenReturn(
			Mono.just(CommunicationModel().apply {
				lastEdition = HistoryModel(dateTime = ZonedDateTime.parse("2026-10-13T10:02:11Z"))
			}),
		)

		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_COMMUNICATION_U),
				buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION)
			)
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.header("If-Match", "\"1791885731000\"")
			.bodyValue(communication)
			.exchange()

		// Assert
		result.body<CommunicationReaderDto>(OK)

		verify(service).updateCommunicationById(any(), eq(defaultProjectId), eq(uuid), any())
		verify(readerMapper).toDto(any())
		verify(writerMapper).toModel(any(), eq(defaultProjectId))
	}

	@Test
	fun `Should updateCommunicationById throw when the element changed since it was read`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val communication =
			CommunicationWriterDto(dateTime = ZonedDateTime.now(ZoneOffset.UTC), alertId = null, movementId = defaultMovementId)

		whenever(
			service.updateCommunicationById(
				any(),
				any(),
				any(),
				any()
			)
		).thenReturn(Mono.just(CommunicationModel()))
		whenever(writerMapper.toModel(any(), any())).thenReturn(CommunicationModel())
		whenever(service.findCommunicationById(any(), any(), anyOrNull())).thenReturn(
			Mono.just(CommunicationModel().apply {
				lastEdition = HistoryModel(dateTime = ZonedDateTime.parse("2026-10-13T10:02:11Z"))
			}),
		)

		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_COMMUNICATION_U),
				buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION)
			)
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.header("If-Match", "\"1\"")
			.bodyValue(communication)
			.exchange()

		// Assert
		result.assertError(PRECONDITION_FAILED, RESOURCE_MODIFIED_SINCE_READ)

		verify(service, never()).updateCommunicationById(any(), eq(defaultProjectId), eq(uuid), any())
	}

	@ParameterizedTest
	@MethodSource("Wrong CommunicationDto")
	fun `Should updateCommunicationById return 400`(
		communication: CommunicationWriterDto,
		expectedCode: String,
	) {
		// Arrange
		val uuid = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate()
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.bodyValue(communication)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedCode)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should disableCommunicationById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(service.disableCommunicationById(any(), eq(defaultProjectId), eq(uuid))).thenReturn(
			Mono.just(
				CommunicationModel()
			)
		)
		whenever(readerMapper.toDto(any())).thenReturn(CommunicationReaderDto())

		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_COMMUNICATION_U),
				buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION)
			)
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<CommunicationReaderDto>(OK)

		verify(service).disableCommunicationById(any(), eq(defaultProjectId), eq(uuid))
		verify(readerMapper).toDto(any())
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should enableCommunicationById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(service.enableCommunicationById(any(), eq(defaultProjectId), eq(uuid))).thenReturn(
			Mono.just(
				CommunicationModel()
			)
		)
		whenever(readerMapper.toDto(any())).thenReturn(CommunicationReaderDto())

		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_COMMUNICATION_D),
				buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION)
			)
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<CommunicationReaderDto>(OK)

		verify(service).enableCommunicationById(any(), eq(defaultProjectId), eq(uuid))
		verify(readerMapper).toDto(any())
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should deleteCommunicationById return 204`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(service.deleteCommunicationById(any(), any(), any())).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_COMMUNICATION_D),
				buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION)
			)
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(NO_CONTENT)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(writerMapper)
		verify(service).deleteCommunicationById(any(), eq(defaultProjectId), eq(uuid))
	}

	@Test
	fun `Should searchMovements return 200`() {
		// Arrange
		whenever(service.searchOutMovementWithActivityByText(any(), anyOrNull())).thenReturn(Flux.empty())

		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_COMMUNICATION_METADATA_R),
				buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION)
			)
			.get()
			.uri(uriBuilder("$BASE_URL/search/movements", listOf(defaultProjectId), listOf(Pair("q", "a"))))
			.exchange()

		// Assert
		result.body<List<*>>(OK)

		verify(service).searchOutMovementWithActivityByText(defaultProjectId, "a")
	}

	@Test
	fun `Should searchMovements throw due to missing communication option`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_COMMUNICATION_METADATA_R))
			.get()
			.uri(uriBuilder("$BASE_URL/search/movements", listOf(defaultProjectId), listOf(Pair("q", "a"))))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)

		verifyNoInteractions(service)
	}

	@Test
	fun `Should searchAlerts return 200`() {
		// Arrange
		whenever(service.searchAlertByText(any(), anyOrNull())).thenReturn(Flux.empty())

		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_COMMUNICATION_METADATA_R),
				buildAuthority(REGISTRY_PROJECT_OPTION_ALERT)
			)
			.get()
			.uri(uriBuilder("$BASE_URL/search/alerts", listOf(defaultProjectId), listOf(Pair("q", "a"))))
			.exchange()

		// Assert
		result.body<List<*>>(OK)

		verify(service).searchAlertByText(defaultProjectId, "a")
	}

	@Test
	fun `Should searchAlerts throw due to missing alert option`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_COMMUNICATION_METADATA_R))
			.get()
			.uri(uriBuilder("$BASE_URL/search/alerts", listOf(defaultProjectId), listOf(Pair("q", "a"))))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)

		verifyNoInteractions(service)
	}
}
