package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_DATETIME_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MESSAGE_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MOVEMENT_OR_ALERT_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TYPE_MISMATCH
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_U
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_COMMUNICATION
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.ICommunicationService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.CommunicationWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.CommunicationReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.CommunicationWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.movementId
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Objects
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
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
import org.testcontainers.shaded.com.google.common.net.HttpHeaders.ACCEPT_LANGUAGE
import reactor.core.publisher.Mono

class CommunicationControllerTest: TestContext() {
	@MockitoBean
	private lateinit var service: ICommunicationService

	@MockitoBean
	private lateinit var readerMapper: CommunicationReaderDtoMapper

	@MockitoBean
	private lateinit var writerMapper: CommunicationWriterDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v1/projects/{projectId}/communications"

		@JvmStatic
		fun `Should findCommunications prepare param, call service and finally cast the result`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of("not locale", null, null, null, null, null, null),
				Arguments.of(null, 0, null, null, null, null, null),
				Arguments.of(null, null, 200, null, null, null, null),
				Arguments.of(null, null, null, null, null, null, null),
				Arguments.of(null, null, null, "text", null, null, null),
				Arguments.of(null, null, null, null, true, null, null),
				Arguments.of(null, null, null, null, null, "2024-11-14T18:34:33.000Z", null),
				Arguments.of(null, null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
			)
		}

		@JvmStatic
		fun `Should findCommunications throw due to wrong params`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of("not uuid", null, null, null, null, null, null, PARAMETER_TYPE_MISMATCH),
				Arguments.of(projectId.toString(), -1, null, null, null, null, null, PAGE_NUMBER_IS_LOWER_THAN_ZERO),
				Arguments.of(projectId.toString(), null, 0, null, null, null, null, PAGE_SIZE_IS_LOWER_THAN_ONE),
				Arguments.of(
					projectId.toString(),
					null,
					201,
					null,
					null,
					null,
					null,
					PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
				),
				Arguments.of(
					projectId.toString(),
					null,
					null,
					null,
					"not boolean",
					null,
					null,
					PARAMETER_TYPE_MISMATCH
				),
				Arguments.of(
					projectId.toString(),
					null,
					null,
					null,
					null,
					"2024/11/14 18:34:33",
					null,
					PARAMETER_TYPE_MISMATCH
				),
				Arguments.of(
					projectId.toString(),
					null,
					null,
					null,
					null,
					null,
					"2024/11/14 18:34:33",
					PARAMETER_TYPE_MISMATCH
				),
			)
		}

		@JvmStatic
		fun `Wrong CommunicationDto`(): Stream<Arguments> = Stream.of(
			Arguments.of(
				CommunicationWriterDto(
					dateTime = null,
					alertId = null,
					movementId = movementId,
				),
				COMMUNICATION_DATETIME_NULL
			),
			Arguments.of(
				CommunicationWriterDto(
					dateTime = ZonedDateTime.now(),
					alertId = null,
					movementId = null,
				),
				COMMUNICATION_MOVEMENT_OR_ALERT_NULL
			),
			Arguments.of(
				CommunicationWriterDto(
					dateTime = ZonedDateTime.now(),
					message = "Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message, Message.",
					alertId = null,
					movementId = movementId,
				),
				COMMUNICATION_MESSAGE_TOO_LONG
			),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findCommunications prepare param, call service and finally cast the result`(
		requestedLocale: String?,
		pageNumber: Int?,
		pageSize: Int?,
		textSearched: String?,
		visibilitySearched: Boolean?,
		startDateTimeSearched: String?,
		endDateTimeSearched: String?,
	) {
		// Arrange
		val expectedPageNumber = pageNumber ?: 0
		val expectedPageSize = pageSize ?: 20
		val pageable = PageableModel(expectedPageNumber * expectedPageSize, expectedPageSize)
		val searchParams = CommunicationSearchParamModel(
			textSearched = textSearched,
			visibilitySearched = visibilitySearched,
			startDateTimeSearched = startDateTimeSearched?.let {
				ZonedDateTime.parse(
					it,
					DateTimeFormatter.ISO_DATE_TIME
				)
			},
			endDateTimeSearched = endDateTimeSearched?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
		)
		val page = PageModel(pageable, totalElements = 1, listOf(CommunicationModel()))
		whenever(service.findCommunicationPage(any(), any(), any())).thenReturn(Mono.just(page))
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
						Pair("pageNumber", pageNumber),
						Pair("pageSize", pageSize),
						Pair("textSearched", textSearched),
						Pair("visibilitySearched", visibilitySearched),
						Pair("startDateTimeSearched", startDateTimeSearched),
						Pair("endDateTimeSearched", endDateTimeSearched),
					),
				)
			)
			.header(ACCEPT_LANGUAGE, requestedLocale)
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findCommunicationPage(projectId, pageable, searchParams)
		verify(readerMapper).toDtoPage(page)
		verifyNoInteractions(writerMapper)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findCommunications throw due to wrong params`(
		communicationProjectId: String?,
		pageNumber: Int?,
		pageSize: Int?,
		textSearched: String?,
		visibilitySearched: String?,
		startDateTimeSearched: String?,
		endDateTimeSearched: String?,
		expectedMessage: String,
	) {
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
					if (Objects.nonNull(communicationProjectId)) listOf(communicationProjectId!!) else emptyList(),
					listOf(
						Pair("pageNumber", pageNumber),
						Pair("pageSize", pageSize),
						Pair("textSearched", textSearched),
						Pair("visibilitySearched", visibilitySearched),
						Pair("startDateTimeSearched", startDateTimeSearched),
						Pair("endDateTimeSearched", endDateTimeSearched),
					),
				)
			)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedMessage)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(writerMapper)
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
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_COMMUNICATION_R),
				buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION)
			)
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<CommunicationReaderDto>(OK)

		verify(service).findCommunicationById(projectId, uuid, visibilitySearched = null)
		verify(readerMapper).toDto(any())
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should createCommunication return 200`() {
		// Arrange
		val communication =
			CommunicationWriterDto(dateTime = ZonedDateTime.now(), alertId = null, movementId = movementId)

		whenever(service.createCommunication(any(), any())).thenReturn(Mono.just(CommunicationModel()))
		whenever(readerMapper.toDto(any())).thenReturn(CommunicationReaderDto())
		whenever(writerMapper.toModel(any(), any())).thenReturn(CommunicationModel())

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
		verify(writerMapper).toModel(any(), eq(projectId))
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
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
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
			CommunicationWriterDto(dateTime = ZonedDateTime.now(), alertId = null, movementId = movementId)

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
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.bodyValue(communication)
			.exchange()

		// Assert
		result.body<CommunicationReaderDto>(OK)

		verify(service).updateCommunicationById(any(), eq(projectId), eq(uuid), any())
		verify(readerMapper).toDto(any())
		verify(writerMapper).toModel(any(), eq(projectId))
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
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
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

		whenever(service.disableCommunicationById(any(), eq(projectId), eq(uuid))).thenReturn(
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
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<CommunicationReaderDto>(OK)

		verify(service).disableCommunicationById(any(), eq(projectId), eq(uuid))
		verify(readerMapper).toDto(any())
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should enableCommunicationById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(service.enableCommunicationById(any(), eq(projectId), eq(uuid))).thenReturn(
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
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<CommunicationReaderDto>(OK)

		verify(service).enableCommunicationById(any(), eq(projectId), eq(uuid))
		verify(readerMapper).toDto(any())
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should deleteCommunicationById return 200`() {
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
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(OK)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(writerMapper)
		verify(service).deleteCommunicationById(any(), eq(projectId), eq(uuid))
	}
}
