package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_DATETIME_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_U
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ALERT
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_COMMUNICATION
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.ICommunicationService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AlertReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.CommunicationWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.AlertReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.CommunicationReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.CommunicationWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.movementId
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.time.ZonedDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class CommunicationV2ControllerTest: TestContext() {
	@MockitoBean
	private lateinit var service: ICommunicationService

	@MockitoBean
	private lateinit var readerMapper: CommunicationReaderDtoMapper

	@MockitoBean
	private lateinit var readerAlertMapper: AlertReaderDtoMapper

	@MockitoBean
	private lateinit var readerMovementMapper: MovementReaderDtoMapper

	@MockitoBean
	private lateinit var writerMapper: CommunicationWriterDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/projects/{projectId}/communications"
	}

	@Test
	fun `Should findCommunications call service with page, size and empty sort by default`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val page = PageModel(pageable, totalElements = 1, listOf(CommunicationModel()))
		whenever(service.findCommunicationPage(any(), any(), any(), any())).thenReturn(Mono.just(page))
		whenever(readerMapper.toDto(any())).thenReturn(CommunicationReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_COMMUNICATION_R), buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<Map<*, *>>(OK)

		verify(service).findCommunicationPage(
			projectId,
			pageable,
			CommunicationSearchParamModel(textSearched = null, visibilitySearched = null, startDateTimeSearched = null, endDateTimeSearched = null),
			emptyList(),
		)
		verify(readerMapper, atLeastOnce()).toDto(any())
	}

	@Test
	fun `Should findCommunications return 400 when a query param is invalid`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_COMMUNICATION_R), buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), listOf(Pair("page", -1))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PAGE_NUMBER_IS_LOWER_THAN_ZERO)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findCommunications return 400 on an unknown sort field`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_COMMUNICATION_R), buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), listOf(Pair("sort", "unknownField"))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, SORT_FIELD_IS_UNKNOWN)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findCommunicationById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.findCommunicationById(any(), any(), anyOrNull())).thenReturn(Mono.just(CommunicationModel()))
		whenever(readerMapper.toDto(any())).thenReturn(CommunicationReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_COMMUNICATION_R), buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<CommunicationReaderDto>(OK)
		verify(service).findCommunicationById(projectId, uuid, visibilitySearched = null)
	}

	@Test
	fun `Should searchActivities rename textSearched to q`() {
		// Arrange
		val searched = "text"
		whenever(service.searchOutMovementWithActivityByText(any(), anyOrNull())).thenReturn(Flux.just(MovementModel(contentType = REGISTERED)))
		whenever(readerMovementMapper.toDto(any())).thenReturn(MovementReaderDto(contentType = REGISTERED))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_COMMUNICATION_METADATA_R), buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION))
			.get()
			.uri(uriBuilder("$BASE_URL/search/movements", listOf(projectId), listOf(Pair("q", searched))))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(service).searchOutMovementWithActivityByText(projectId, searched)
	}

	@Test
	fun `Should searchAlerts rename textSearched to q`() {
		// Arrange
		val searched = "text"
		whenever(service.searchAlertByText(any(), anyOrNull())).thenReturn(Flux.just(AlertModel()))
		whenever(readerAlertMapper.toDto(any())).thenReturn(AlertReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_COMMUNICATION_METADATA_R), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.get()
			.uri(uriBuilder("$BASE_URL/search/alerts", listOf(projectId), listOf(Pair("q", searched))))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(service).searchAlertByText(projectId, searched)
	}

	@Test
	fun `Should createCommunication return 201 with a Location header`() {
		// Arrange
		val communication = CommunicationWriterDto(dateTime = ZonedDateTime.now(), alertId = null, movementId = movementId)
		val createdId = UUID.randomUUID()
		whenever(service.createCommunication(any(), any())).thenReturn(Mono.just(CommunicationModel().apply { id = createdId }))
		whenever(writerMapper.toModel(any(), any())).thenReturn(CommunicationModel())
		whenever(readerMapper.toDto(any())).thenReturn(CommunicationReaderDto().apply { id = createdId })

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_COMMUNICATION_C), buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(communication)
			.exchange()

		// Assert
		val entityResult = result.expectStatus().isCreated.expectBody(CommunicationReaderDto::class.java).returnResult()
		val location = entityResult.responseHeaders.getFirst("Location")
		assertNotNull(location)
		assertTrue(location!!.endsWith("/api/v2/projects/$projectId/communications/$createdId"))

		verify(service).createCommunication(any(), any())
	}

	@Test
	fun `Should createCommunication return 400`() {
		// Arrange
		val communication = CommunicationWriterDto(dateTime = null, alertId = null, movementId = movementId)

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(communication)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, COMMUNICATION_DATETIME_NULL)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should updateCommunicationById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val communication = CommunicationWriterDto(dateTime = ZonedDateTime.now(), alertId = null, movementId = movementId)
		whenever(service.updateCommunicationById(any(), any(), any(), any())).thenReturn(Mono.just(CommunicationModel()))
		whenever(writerMapper.toModel(any(), any())).thenReturn(CommunicationModel())
		whenever(readerMapper.toDto(any())).thenReturn(CommunicationReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_COMMUNICATION_U), buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.bodyValue(communication)
			.exchange()

		// Assert
		result.body<CommunicationReaderDto>(OK)
		verify(service).updateCommunicationById(any(), eq(projectId), eq(uuid), any())
	}

	@Test
	fun `Should disableCommunicationById use POST and return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.disableCommunicationById(any(), eq(projectId), eq(uuid))).thenReturn(Mono.just(CommunicationModel()))
		whenever(readerMapper.toDto(any())).thenReturn(CommunicationReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_COMMUNICATION_U), buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<CommunicationReaderDto>(OK)
		verify(service).disableCommunicationById(any(), eq(projectId), eq(uuid))
	}

	@Test
	fun `Should enableCommunicationById use POST and return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.enableCommunicationById(any(), eq(projectId), eq(uuid))).thenReturn(Mono.just(CommunicationModel()))
		whenever(readerMapper.toDto(any())).thenReturn(CommunicationReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_COMMUNICATION_U), buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<CommunicationReaderDto>(OK)
		verify(service).enableCommunicationById(any(), eq(projectId), eq(uuid))
	}

	@Test
	fun `Should deleteCommunicationById return 204`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.deleteCommunicationById(any(), any(), any())).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_COMMUNICATION_D), buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(NO_CONTENT)
		verify(service).deleteCommunicationById(any(), eq(projectId), eq(uuid))
	}
}
