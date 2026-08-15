package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_COMMUNICATION_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_U
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ALERT
import fr.laucoin.registry.backend.domain.enumeration.AlertSortFieldEnum
import fr.laucoin.registry.backend.domain.enumeration.AlertSortFieldEnum.DATE_TIME
import fr.laucoin.registry.backend.domain.enumeration.AlertSortFieldEnum.TITLE
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum.CANCELED
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum.IN_PROGRESS
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum.RESOLVED
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.service.IAlertService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.AlertReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.AlertCreationWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.AlertWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.AlertReaderDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.commonCommunication
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals

class AlertV2ControllerTest : TestContext() {
	@MockitoBean
	private lateinit var service: IAlertService

	@MockitoBean
	private lateinit var readerMapper: AlertReaderDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/projects/{projectId}/alerts"
	}

	@Test
	fun `Should findAlerts return 200 with the v2 list grammar`() {
		// Arrange
		val pageable = PageableModel(20, 10)
		val page = PageModel(pageable, totalElements = 1, listOf(AlertModel()))
		whenever(service.findAlertsPage(any(), any(), any(), any())).thenReturn(Mono.just(page))
		whenever(readerMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(AlertReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_R), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(projectId),
					listOf(
						Pair("page", 2),
						Pair("size", 10),
						Pair("q", "fire"),
						Pair("visible", true),
						Pair("status", IN_PROGRESS),
						Pair("sort", "dateTime,title"),
						Pair("direction", "DESC"),
					),
				)
			)
			.exchange()

		// Assert
		result.body<PageModel<AlertReaderDto>>(OK)
		val pageableCaptor = argumentCaptor<PageableModel>()
		val sortCaptor = argumentCaptor<List<SortModel<AlertSortFieldEnum>>>()
		verify(service).findAlertsPage(eq(projectId), pageableCaptor.capture(), any(), sortCaptor.capture())
		assertEquals(PageableModel(offset = 20, limit = 10), pageableCaptor.firstValue)
		assertEquals(listOf(SortModel(DATE_TIME, descending = true), SortModel(TITLE, descending = true)), sortCaptor.firstValue)
	}

	@Test
	fun `Should findAlerts reject an unknown sort field with 400`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_R), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), listOf(Pair("sort", "severity"))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, SORT_FIELD_IS_UNKNOWN)
	}

	@Test
	fun `Should findAlerts return 403 without the read authority`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should resolveAlertById act as an explicit POST transition`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.updateAlertStatusById(any(), any(), any(), any())).thenReturn(Mono.just(AlertModel()))
		whenever(readerMapper.toDto(any())).thenReturn(AlertReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_U), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/resolve", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<AlertReaderDto>(OK)
		verify(service).updateAlertStatusById(any(), eq(projectId), eq(id), eq(RESOLVED))
	}

	@Test
	fun `Should cancelAlertById act as an explicit POST transition`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.updateAlertStatusById(any(), any(), any(), any())).thenReturn(Mono.just(AlertModel()))
		whenever(readerMapper.toDto(any())).thenReturn(AlertReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_U), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/cancel", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<AlertReaderDto>(OK)
		verify(service).updateAlertStatusById(any(), eq(projectId), eq(id), eq(CANCELED))
	}

	@Test
	fun `Should reopenAlertById act as an explicit POST transition`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.updateAlertStatusById(any(), any(), any(), any())).thenReturn(Mono.just(AlertModel()))
		whenever(readerMapper.toDto(any())).thenReturn(AlertReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_U), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/reopen", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<AlertReaderDto>(OK)
		verify(service).updateAlertStatusById(any(), eq(projectId), eq(id), eq(IN_PROGRESS))
	}

	@Test
	fun `Should resolveAlertById return 403 without the update authority`() {
		// Arrange
		val id = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/resolve", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should disableAlertById act as an explicit POST transition`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.disableAlertById(any(), any(), any())).thenReturn(Mono.just(AlertModel()))
		whenever(readerMapper.toDto(any())).thenReturn(AlertReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_U), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<AlertReaderDto>(OK)
		verify(service).disableAlertById(any(), eq(projectId), eq(id))
	}

	@Test
	fun `Should enableAlertById act as an explicit POST transition`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.enableAlertById(any(), any(), any())).thenReturn(Mono.just(AlertModel()))
		whenever(readerMapper.toDto(any())).thenReturn(AlertReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_U), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<AlertReaderDto>(OK)
		verify(service).enableAlertById(any(), eq(projectId), eq(id))
	}

	@Test
	fun `Should findAlertById return 200`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.findAlertById(any(), any(), anyOrNull())).thenReturn(Mono.just(AlertModel()))
		whenever(readerMapper.toDto(any())).thenReturn(AlertReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_R), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<AlertReaderDto>(OK)
		verify(service).findAlertById(projectId, id, null)
		verify(readerMapper).toDto(any())
	}

	@Test
	fun `Should findAlertById return 403 without the read authority`() {
		// Arrange
		val id = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should findAlertCommunications return 200 with the v2 list grammar`() {
		// Arrange
		val id = UUID.randomUUID()
		val page = PageModel(PageableModel(20, 10), totalElements = 1, listOf(commonCommunication()))
		whenever(service.findAlertCommunicationsPage(any(), any(), any(), any())).thenReturn(Mono.just(page))

		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_ALERT_COMMUNICATION_R),
				buildAuthority(REGISTRY_PROJECT_OPTION_ALERT),
			)
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/{id}/communications",
					listOf(projectId, id),
					listOf(Pair("page", 2), Pair("size", 10)),
				)
			)
			.exchange()

		// Assert
		result.body<PageModel<CommunicationReaderDto>>(OK)
		val pageableCaptor = argumentCaptor<PageableModel>()
		verify(service).findAlertCommunicationsPage(eq(projectId), eq(id), pageableCaptor.capture(), any())
		assertEquals(PageableModel(offset = 20, limit = 10), pageableCaptor.firstValue)
	}

	@Test
	fun `Should findAlertCommunications return 403 without the communication read authority`() {
		// Arrange
		val id = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_R), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}/communications", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should createAlert return 200`() {
		// Arrange
		val alert = AlertCreationWriterDto(
			title = "Fire",
			dateTime = ZonedDateTime.now(),
			message = "message",
			movementId = null,
		)
		whenever(service.createAlert(any(), any())).thenReturn(Mono.just(AlertModel()))
		whenever(readerMapper.toDto(any())).thenReturn(AlertReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_C), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(alert)
			.exchange()

		// Assert
		result.body<AlertReaderDto>(OK)
		verify(service).createAlert(any(), any())
		verify(readerMapper).toDto(any())
	}

	@Test
	fun `Should createAlert return 403 without the create authority`() {
		// Arrange
		val alert = AlertCreationWriterDto(
			title = "Fire",
			dateTime = ZonedDateTime.now(),
			message = "message",
			movementId = null,
		)

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(alert)
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should updateAlertById return 200`() {
		// Arrange
		val id = UUID.randomUUID()
		val alert = AlertWriterDto(title = "Fire", dateTime = ZonedDateTime.now(), status = IN_PROGRESS)
		whenever(service.updateAlertById(any(), any(), any(), any())).thenReturn(Mono.just(AlertModel()))
		whenever(readerMapper.toDto(any())).thenReturn(AlertReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_U), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.bodyValue(alert)
			.exchange()

		// Assert
		result.body<AlertReaderDto>(OK)
		verify(service).updateAlertById(any(), eq(projectId), eq(id), any())
	}

	@Test
	fun `Should updateAlertById return 403 without the update authority`() {
		// Arrange
		val id = UUID.randomUUID()
		val alert = AlertWriterDto(title = "Fire", dateTime = ZonedDateTime.now(), status = IN_PROGRESS)

		// Act
		val result = webClient
			.authenticate()
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.bodyValue(alert)
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should deleteAlertById return 200`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.deleteAlertById(any(), any(), any())).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_D), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(OK)
		verify(service).deleteAlertById(any(), eq(projectId), eq(id))
	}

	@Test
	fun `Should deleteAlertById return 403 without the delete authority`() {
		// Arrange
		val id = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_U), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
		verifyNoInteractions(service)
	}
}
