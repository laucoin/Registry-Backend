package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AlertError.ALERT_TITLE_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_COMMUNICATION_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_U
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ALERT
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum.IN_PROGRESS
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.IAlertService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AlertReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.AlertCreationWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.AlertStatusWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.AlertWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.AlertReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.CommunicationReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.AlertCreationWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.AlertWriterDtoMapper
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
import reactor.core.publisher.Mono

class AlertV2ControllerTest: TestContext() {
	@MockitoBean
	private lateinit var service: IAlertService

	@MockitoBean
	private lateinit var readerMapper: AlertReaderDtoMapper

	@MockitoBean
	private lateinit var communicationReaderMapper: CommunicationReaderDtoMapper

	@MockitoBean
	private lateinit var writerMapper: AlertWriterDtoMapper

	@MockitoBean
	private lateinit var creationWriterMapper: AlertCreationWriterDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/projects/{projectId}/alerts"
	}

	@Test
	fun `Should findAlerts call service with page, size and empty sort by default`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val page = PageModel(pageable, totalElements = 1, listOf(AlertModel()))
		whenever(service.findAlertsPage(any(), any(), any(), any())).thenReturn(Mono.just(page))
		whenever(readerMapper.toDto(any())).thenReturn(AlertReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_R), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<Map<*, *>>(OK)

		verify(service).findAlertsPage(
			projectId,
			pageable,
			AlertSearchParamModel(textSearched = null, visibilitySearched = null, statusSearched = null, startDateTimeSearched = null, endDateTimeSearched = null),
			emptyList(),
		)
		verify(readerMapper, atLeastOnce()).toDto(any())
	}

	@Test
	fun `Should findAlerts return 400 when a query param is invalid`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_R), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), listOf(Pair("page", -1))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PAGE_NUMBER_IS_LOWER_THAN_ZERO)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findAlerts return 400 on an unknown sort field`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_R), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), listOf(Pair("sort", "unknownField"))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, SORT_FIELD_IS_UNKNOWN)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findAlertById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.findAlertById(any(), any(), anyOrNull())).thenReturn(Mono.just(AlertModel()))
		whenever(readerMapper.toDto(any())).thenReturn(AlertReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_R), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<AlertReaderDto>(OK)
		verify(service).findAlertById(projectId, uuid, visibilitySearched = null)
	}

	@Test
	fun `Should findAlertCommunications drop the Searched suffix and call service`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val pageable = PageableModel(0, 20)
		val page = PageModel(pageable, totalElements = 0, emptyList<CommunicationModel>())
		whenever(service.findAlertCommunicationsPage(any(), any(), any(), any())).thenReturn(Mono.just(page))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_COMMUNICATION_R), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}/communications", listOf(projectId, uuid), listOf(Pair("visible", true))))
			.exchange()

		// Assert
		result.body<Map<*, *>>(OK)
		verify(service).findAlertCommunicationsPage(eq(projectId), eq(uuid), eq(pageable), any())
	}

	@Test
	fun `Should createAlert return 201 with a Location header`() {
		// Arrange
		val alert = AlertCreationWriterDto(title = "Alert 1", dateTime = ZonedDateTime.now(), message = "test", movementId = null)
		val createdId = UUID.randomUUID()
		whenever(service.createAlert(any(), any())).thenReturn(Mono.just(AlertModel().apply { id = createdId }))
		whenever(creationWriterMapper.toModel(any(), any())).thenReturn(AlertModel())
		whenever(readerMapper.toDto(any())).thenReturn(AlertReaderDto().apply { id = createdId })

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_C), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(alert)
			.exchange()

		// Assert
		val entityResult = result.expectStatus().isCreated.expectBody(AlertReaderDto::class.java).returnResult()
		val location = entityResult.responseHeaders.getFirst("Location")
		assertNotNull(location)
		assertTrue(location!!.endsWith("/api/v2/projects/$projectId/alerts/$createdId"))

		verify(service).createAlert(any(), any())
	}

	@Test
	fun `Should createAlert return 400`() {
		// Arrange
		val alert = AlertCreationWriterDto(title = null, dateTime = ZonedDateTime.now(), message = "test", movementId = null)

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(alert)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, ALERT_TITLE_NULL_OR_BLANK)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should updateAlertById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val alert = AlertWriterDto(title = "Alert 1", dateTime = ZonedDateTime.now(), status = IN_PROGRESS)
		whenever(service.updateAlertById(any(), any(), any(), any())).thenReturn(Mono.just(AlertModel()))
		whenever(writerMapper.toModel(any(), any())).thenReturn(AlertModel())
		whenever(readerMapper.toDto(any())).thenReturn(AlertReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_U), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.bodyValue(alert)
			.exchange()

		// Assert
		result.body<AlertReaderDto>(OK)
		verify(service).updateAlertById(any(), eq(projectId), eq(uuid), any())
	}

	@Test
	fun `Should updateAlertStatusById take status in the body instead of the path`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.updateAlertStatusById(any(), any(), any(), any())).thenReturn(Mono.just(AlertModel()))
		whenever(readerMapper.toDto(any())).thenReturn(AlertReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_U), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/status", listOf(projectId, uuid), emptyList()))
			.bodyValue(AlertStatusWriterDto(status = IN_PROGRESS))
			.exchange()

		// Assert
		result.body<AlertReaderDto>(OK)
		verify(service).updateAlertStatusById(any(), eq(projectId), eq(uuid), eq(IN_PROGRESS))
	}

	@Test
	fun `Should disableAlertById use POST and return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.disableAlertById(any(), eq(projectId), eq(uuid))).thenReturn(Mono.just(AlertModel()))
		whenever(readerMapper.toDto(any())).thenReturn(AlertReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_U), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<AlertReaderDto>(OK)
		verify(service).disableAlertById(any(), eq(projectId), eq(uuid))
	}

	@Test
	fun `Should enableAlertById use POST and return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.enableAlertById(any(), eq(projectId), eq(uuid))).thenReturn(Mono.just(AlertModel()))
		whenever(readerMapper.toDto(any())).thenReturn(AlertReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_U), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<AlertReaderDto>(OK)
		verify(service).enableAlertById(any(), eq(projectId), eq(uuid))
	}

	@Test
	fun `Should deleteAlertById return 204`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.deleteAlertById(any(), any(), any())).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_D), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(NO_CONTENT)
		verify(service).deleteAlertById(any(), eq(projectId), eq(uuid))
	}
}
