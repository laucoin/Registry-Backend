package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_U
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_COMMUNICATION
import fr.laucoin.registry.backend.domain.enumeration.CommunicationSortFieldEnum
import fr.laucoin.registry.backend.domain.enumeration.CommunicationSortFieldEnum.DATE_TIME
import fr.laucoin.registry.backend.domain.enumeration.CommunicationSortFieldEnum.MESSAGE
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.service.ICommunicationService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.CommunicationWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.CommunicationReaderDtoMapper
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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals

class CommunicationV2ControllerTest : TestContext() {
	@MockitoBean
	private lateinit var service: ICommunicationService

	@MockitoBean
	private lateinit var readerMapper: CommunicationReaderDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/projects/{projectId}/communications"
	}

	@Test
	fun `Should findCommunications return 200 with the v2 list grammar`() {
		// Arrange
		val pageable = PageableModel(20, 10)
		val page = PageModel(pageable, totalElements = 1, listOf(CommunicationModel()))
		whenever(service.findCommunicationPage(any(), any(), any(), any())).thenReturn(Mono.just(page))
		whenever(readerMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(CommunicationReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_COMMUNICATION_R),
				buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION)
			)
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(projectId),
					listOf(
						Pair("page", 2),
						Pair("size", 10),
						Pair("q", "hello"),
						Pair("visible", true),
						Pair("sort", "dateTime,message"),
						Pair("direction", "DESC"),
					),
				)
			)
			.exchange()

		// Assert
		result.body<PageModel<CommunicationReaderDto>>(OK)
		val pageableCaptor = argumentCaptor<PageableModel>()
		val sortCaptor = argumentCaptor<List<SortModel<CommunicationSortFieldEnum>>>()
		verify(service).findCommunicationPage(eq(projectId), pageableCaptor.capture(), any(), sortCaptor.capture())
		assertEquals(PageableModel(offset = 20, limit = 10), pageableCaptor.firstValue)
		assertEquals(listOf(SortModel(DATE_TIME, descending = true), SortModel(MESSAGE, descending = true)), sortCaptor.firstValue)
	}

	@Test
	fun `Should findCommunications reject an unknown sort field with 400`() {
		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_COMMUNICATION_R),
				buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION)
			)
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), listOf(Pair("sort", "author"))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, SORT_FIELD_IS_UNKNOWN)
	}

	@Test
	fun `Should findCommunications return 403 without the read authority`() {
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
	fun `Should getAttachableMovements search the eligibility sub-collection with q`() {
		// Arrange
		whenever(service.searchOutMovementWithActivityByText(any(), any())).thenReturn(Flux.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_COMMUNICATION_METADATA_R))
			.get()
			.uri(uriBuilder("$BASE_URL/attachable-movements", listOf(projectId), listOf(Pair("q", "sortie"))))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(service).searchOutMovementWithActivityByText(projectId, "sortie")
	}

	@Test
	fun `Should getAttachableAlerts search the eligibility sub-collection with q`() {
		// Arrange
		whenever(service.searchAlertByText(any(), any())).thenReturn(Flux.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_COMMUNICATION_METADATA_R))
			.get()
			.uri(uriBuilder("$BASE_URL/attachable-alerts", listOf(projectId), listOf(Pair("q", "fire"))))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(service).searchAlertByText(projectId, "fire")
	}

	@Test
	fun `Should disableCommunicationById act as an explicit POST transition`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.disableCommunicationById(any(), any(), any())).thenReturn(Mono.just(CommunicationModel()))
		whenever(readerMapper.toDto(any())).thenReturn(CommunicationReaderDto())

		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_COMMUNICATION_U),
				buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION)
			)
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<CommunicationReaderDto>(OK)
		verify(service).disableCommunicationById(any(), eq(projectId), eq(id))
	}

	@Test
	fun `Should enableCommunicationById act as an explicit POST transition`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.enableCommunicationById(any(), any(), any())).thenReturn(Mono.just(CommunicationModel()))
		whenever(readerMapper.toDto(any())).thenReturn(CommunicationReaderDto())

		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_COMMUNICATION_U),
				buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION)
			)
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<CommunicationReaderDto>(OK)
		verify(service).enableCommunicationById(any(), eq(projectId), eq(id))
	}

	@Test
	fun `Should findCommunicationById return 200`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.findCommunicationById(any(), any(), anyOrNull())).thenReturn(Mono.just(CommunicationModel()))
		whenever(readerMapper.toDto(any())).thenReturn(CommunicationReaderDto())

		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_COMMUNICATION_R),
				buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION)
			)
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<CommunicationReaderDto>(OK)
		verify(service).findCommunicationById(projectId, id, null)
		verify(readerMapper).toDto(any())
	}

	@Test
	fun `Should findCommunicationById return 403 without the read authority`() {
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
	fun `Should createCommunication return 200`() {
		// Arrange
		val communication = CommunicationWriterDto(
			dateTime = ZonedDateTime.now(),
			message = "hello",
			movementId = UUID.randomUUID(),
			alertId = null,
		)
		whenever(service.createCommunication(any(), any())).thenReturn(Mono.just(CommunicationModel()))
		whenever(readerMapper.toDto(any())).thenReturn(CommunicationReaderDto())

		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_COMMUNICATION_C),
				buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION)
			)
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(communication)
			.exchange()

		// Assert
		result.body<CommunicationReaderDto>(OK)
		verify(service).createCommunication(any(), any())
		verify(readerMapper).toDto(any())
	}

	@Test
	fun `Should createCommunication return 403 without the create authority`() {
		// Arrange
		val communication = CommunicationWriterDto(
			dateTime = ZonedDateTime.now(),
			message = "hello",
			movementId = UUID.randomUUID(),
			alertId = null,
		)

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(communication)
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should updateCommunicationById return 200`() {
		// Arrange
		val id = UUID.randomUUID()
		val communication = CommunicationWriterDto(
			dateTime = ZonedDateTime.now(),
			message = "hello",
			movementId = UUID.randomUUID(),
			alertId = null,
		)
		whenever(service.updateCommunicationById(any(), any(), any(), any()))
			.thenReturn(Mono.just(CommunicationModel()))
		whenever(readerMapper.toDto(any())).thenReturn(CommunicationReaderDto())

		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_COMMUNICATION_U),
				buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION)
			)
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.bodyValue(communication)
			.exchange()

		// Assert
		result.body<CommunicationReaderDto>(OK)
		verify(service).updateCommunicationById(any(), eq(projectId), eq(id), any())
	}

	@Test
	fun `Should updateCommunicationById return 403 without the update authority`() {
		// Arrange
		val id = UUID.randomUUID()
		val communication = CommunicationWriterDto(
			dateTime = ZonedDateTime.now(),
			message = "hello",
			movementId = UUID.randomUUID(),
			alertId = null,
		)

		// Act
		val result = webClient
			.authenticate()
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.bodyValue(communication)
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should deleteCommunicationById return 200`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.deleteCommunicationById(any(), any(), any())).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_COMMUNICATION_D),
				buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION)
			)
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(OK)
		verify(service).deleteCommunicationById(any(), eq(projectId), eq(id))
	}

	@Test
	fun `Should deleteCommunicationById return 403 without the delete authority`() {
		// Arrange
		val id = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_COMMUNICATION_U),
				buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION)
			)
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
		verifyNoInteractions(service)
	}
}
