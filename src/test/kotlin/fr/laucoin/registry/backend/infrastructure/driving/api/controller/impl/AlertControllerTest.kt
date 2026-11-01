package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AlertError.ALERT_STATUS_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AlertError.ALERT_TITLE_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AlertError.ALERT_TITLE_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_DATETIME_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MESSAGE_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.DATE_RANGE_FROM_LATER_THAN_TO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_UPPER_THAN_MAX_PAGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TYPE_MISMATCH
import fr.laucoin.registry.backend.domain.constant.ErrorConst.RESOURCE_MODIFIED_SINCE_READ
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_COMMUNICATION_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_U
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ALERT
import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum
import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum.HIGH
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum.IN_PROGRESS
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.IAlertService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AlertReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.AlertCreationWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.AlertWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.AlertReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.CommunicationReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.AlertCreationWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.AlertWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.hiddenElementsAuthorities
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
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
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.PRECONDITION_FAILED
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.shaded.com.google.common.net.HttpHeaders.ACCEPT_LANGUAGE
import reactor.core.publisher.Mono
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Objects
import java.util.UUID
import java.util.stream.Stream

class AlertControllerTest : TestContext() {
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

		@JvmStatic
		fun `Should findAlerts prepare param, call service and finally cast the result`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of("not locale", null, null, null, null, null, null, null, null),
				Arguments.of(null, 0, null, null, null, null, null, null, null),
				Arguments.of(null, null, 200, null, null, null, null, null, null),
				Arguments.of(null, null, null, null, null, null, null, null, null),
				Arguments.of(null, null, null, "text", null, null, null, null, null),
				Arguments.of(null, null, null, null, true, null, null, null, null),
				Arguments.of(null, null, null, null, null, IN_PROGRESS, null, null, null),
				Arguments.of(null, null, null, null, null, null, HIGH, null, null),
				Arguments.of(null, null, null, null, null, null, null, "2024-11-14T18:34:33.000Z", null),
				Arguments.of(null, null, null, null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
			)
		}

		@JvmStatic
		fun `Should findAlerts throw due to wrong params`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of("not uuid", null, null, null, null, null, null, null, PARAMETER_TYPE_MISMATCH),
				Arguments.of(
					defaultProjectId.toString(),
					-1,
					null,
					null,
					null,
					null,
					null,
					null,
					PAGE_NUMBER_IS_LOWER_THAN_ZERO
				),
				Arguments.of(
					defaultProjectId.toString(),
					null,
					0,
					null,
					null,
					null,
					null,
					null,
					PAGE_SIZE_IS_LOWER_THAN_ONE
				),
				Arguments.of(
					defaultProjectId.toString(),
					null,
					201,
					null,
					null,
					null,
					null,
					null,
					PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
				),
				Arguments.of(
					defaultProjectId.toString(),
					null,
					null,
					null,
					"not boolean",
					null,
					null,
					null,
					PARAMETER_TYPE_MISMATCH
				),
				Arguments.of(
					defaultProjectId.toString(),
					null,
					null,
					null,
					null,
					"not boolean",
					null,
					null,
					PARAMETER_TYPE_MISMATCH
				),
				Arguments.of(
					defaultProjectId.toString(),
					null,
					null,
					null,
					null,
					null,
					"2024/11/14 18:34:33",
					null,
					PARAMETER_TYPE_MISMATCH
				),
				Arguments.of(
					defaultProjectId.toString(),
					null,
					null,
					null,
					null,
					null,
					null,
					"2024/11/14 18:34:33",
					PARAMETER_TYPE_MISMATCH
				),
				Arguments.of(
					defaultProjectId.toString(),
					null,
					null,
					null,
					null,
					null,
					"2024-02-01T00:00:00Z",
					"2024-01-01T00:00:00Z",
					DATE_RANGE_FROM_LATER_THAN_TO
				),
			)
		}

		@JvmStatic
		fun `Should findAlertCommunications prepare param, call service and finally cast the result`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(null, null, null, null, null, null),
				Arguments.of(0, null, null, null, null, null),
				Arguments.of(null, 200, null, null, null, null),
				Arguments.of(null, null, "test", null, null, null),
				Arguments.of(null, null, null, true, null, null),
				Arguments.of(null, null, null, null, "2024-11-14T18:34:33.000Z", null),
				Arguments.of(null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
			)
		}

		@JvmStatic
		fun `Should findAlertCommunications throw due to wrong params`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(-1, null, PAGE_NUMBER_IS_LOWER_THAN_ZERO),
				Arguments.of(1_000_001, null, PAGE_NUMBER_IS_UPPER_THAN_MAX_PAGE),
				Arguments.of(null, 0, PAGE_SIZE_IS_LOWER_THAN_ONE),
				Arguments.of(null, 201, PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE),
			)
		}

		@JvmStatic
		fun `Should createAlert return 400`(): Stream<Arguments> = Stream.of(
			Arguments.of(
				AlertCreationWriterDto(
					title = null,
					dateTime = ZonedDateTime.now(ZoneOffset.UTC),
					message = null,
					movementId = null,
				),
				ALERT_TITLE_NULL_OR_BLANK,
			),
			Arguments.of(
				AlertCreationWriterDto(
					title = "Alert name very loooooooooooooooooooooooooooooooooooooooooooooong",
					dateTime = ZonedDateTime.now(ZoneOffset.UTC),
					message = null,
					movementId = null,
				),
				ALERT_TITLE_TOO_LONG,
			),
			Arguments.of(
				AlertCreationWriterDto(
					title = "Alert 1",
					dateTime = null,
					message = null,
					movementId = null,
				),
				COMMUNICATION_DATETIME_NULL,
			),
			Arguments.of(
				AlertCreationWriterDto(
					title = "Alert 1",
					dateTime = ZonedDateTime.now(ZoneOffset.UTC),
					message = "Message very loooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooong",
					movementId = null,
				),
				COMMUNICATION_MESSAGE_TOO_LONG,
			),
		)

		@JvmStatic
		fun `Should updateAlertById return 400`(): Stream<Arguments> = Stream.of(
			Arguments.of(
				AlertWriterDto(
					title = null,
					dateTime = ZonedDateTime.now(ZoneOffset.UTC),
					status = IN_PROGRESS,
				),
				ALERT_TITLE_NULL_OR_BLANK,
			),
			Arguments.of(
				AlertWriterDto(
					title = "Alert name very loooooooooooooooooooooooooooooooooooooooooooooong",
					dateTime = ZonedDateTime.now(ZoneOffset.UTC),
					status = IN_PROGRESS,
				),
				ALERT_TITLE_TOO_LONG,
			),
			Arguments.of(
				AlertWriterDto(
					title = "Alert 1",
					dateTime = null,
					status = IN_PROGRESS,
				),
				COMMUNICATION_DATETIME_NULL,
			),
			Arguments.of(
				AlertWriterDto(
					title = "Alert 1",
					dateTime = ZonedDateTime.now(ZoneOffset.UTC),
					status = null,
				),
				ALERT_STATUS_NULL,
			),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findAlerts prepare param, call service and finally cast the result`(
		requestedLocale: String?,
		page: Int?,
		size: Int?,
		query: String?,
		isVisible: Boolean?,
		status: AlertStatusEnum?,
		criticality: AlertCriticalityEnum?,
		from: String?,
		to: String?,
	) {
		// Arrange
		val expectedPageNumber = page ?: 0
		val expectedPageSize = size ?: 20
		val pageable = PageableModel(expectedPageNumber.toLong() * expectedPageSize, expectedPageSize)
		val searchParams = AlertSearchParamModel(
			query = query,
			isVisible = isVisible,
			status = status,
			criticality = criticality,
			from = from?.let {
				ZonedDateTime.parse(
					it,
					DateTimeFormatter.ISO_DATE_TIME
				)
			},
			to = to?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
		)
		val expectedPage = PageModel(pageable, totalElements = 1, listOf(AlertModel()))
		whenever(service.findAlertsPage(any(), any(), any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(readerMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(AlertReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(*hiddenElementsAuthorities(), buildAuthority(REGISTRY_PROJECT_ALERT_R), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(defaultProjectId),
					listOf(
						Pair("page", page),
						Pair("size", size),
						Pair("q", query),
						Pair("isVisible", isVisible),
						Pair("status", status),
						Pair("criticality", criticality),
						Pair("from", from),
						Pair("to", to),
					),
				)
			)
			.headers { headers -> requestedLocale?.let { headers.add(ACCEPT_LANGUAGE, it) } }
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findAlertsPage(any(), eq(defaultProjectId), eq(pageable), eq(searchParams))
		verify(readerMapper).toDtoPage(expectedPage)
		verifyNoInteractions(communicationReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findAlerts throw due to wrong params`(
		alertProjectId: String?,
		page: Int?,
		size: Int?,
		query: String?,
		isVisible: String?,
		status: String?,
		from: String?,
		to: String?,
		expectedMessage: String,
	) {
		// Act
		val result = webClient
			.authenticate(*hiddenElementsAuthorities(), buildAuthority(REGISTRY_PROJECT_ALERT_R), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					if (Objects.nonNull(alertProjectId)) listOf(alertProjectId!!) else emptyList(),
					listOf(
						Pair("page", page),
						Pair("size", size),
						Pair("q", query),
						Pair("isVisible", isVisible),
						Pair("status", status),
						Pair("status", status),
						Pair("from", from),
						Pair("to", to),
					),
				)
			)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedMessage)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(communicationReaderMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findAlerts return 400 with an unknown criticality`() {
		// Arrange
		val criticality = "not a criticality"

		// Act
		val result = webClient
			.authenticate(*hiddenElementsAuthorities(), buildAuthority(REGISTRY_PROJECT_ALERT_R), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), listOf(Pair("criticality", criticality))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PARAMETER_TYPE_MISMATCH)

		verifyNoInteractions(service)
	}

	@Test
	fun `Should findAlertById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.findAlertForUser(any(), any(), any())).thenReturn(Mono.just(AlertModel()))
		whenever(readerMapper.toDto(any())).thenReturn(AlertReaderDto())

		// Act
		val result = webClient
			.authenticate(*hiddenElementsAuthorities(), buildAuthority(REGISTRY_PROJECT_ALERT_R), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<AlertReaderDto>(OK)

		verify(service).findAlertForUser(any(), eq(defaultProjectId), eq(uuid))
		verify(readerMapper).toDto(any())
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(communicationReaderMapper)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findAlertCommunications prepare param, call service and finally cast the result`(
		page: Int?,
		size: Int?,
		query: String?,
		isVisible: Boolean?,
		from: String?,
		to: String?,
	) {
		// Arrange
		val uuid = UUID.randomUUID()
		val expectedPageNumber = page ?: 0
		val expectedPageSize = size ?: 20
		val pageable = PageableModel(expectedPageNumber.toLong() * expectedPageSize, expectedPageSize)
		val searchParams = CommunicationSearchParamModel(
			query = query,
			isVisible = isVisible,
			from = from?.let {
				ZonedDateTime.parse(
					it,
					DateTimeFormatter.ISO_DATE_TIME
				)
			},
			to = to?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
		)
		val expectedPage = PageModel(pageable, totalElements = 1, listOf(CommunicationModel()))
		whenever(service.findAlertCommunicationsPage(any(), any(), any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(communicationReaderMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(CommunicationReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(*hiddenElementsAuthorities(), 
				buildAuthority(REGISTRY_PROJECT_ALERT_COMMUNICATION_R),
				buildAuthority(REGISTRY_PROJECT_OPTION_ALERT)
			)
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/{id}/communications",
					listOf(defaultProjectId, uuid),
					listOf(
						Pair("page", page),
						Pair("size", size),
						Pair("q", query),
						Pair("isVisible", isVisible),
						Pair("from", from),
						Pair("to", to),
					),
				)
			)
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findAlertCommunicationsPage(defaultProjectId, uuid, pageable, searchParams)
		verify(communicationReaderMapper).toDtoPage(expectedPage)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(writerMapper)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findAlertCommunications throw due to wrong params`(
		page: Int?,
		size: Int?,
		expectedMessage: String,
	) {
		// Arrange
		val uuid = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate(*hiddenElementsAuthorities(), 
				buildAuthority(REGISTRY_PROJECT_ALERT_COMMUNICATION_R),
				buildAuthority(REGISTRY_PROJECT_OPTION_ALERT)
			)
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/{id}/communications",
					listOf(defaultProjectId, uuid),
					listOf(
						Pair("page", page),
						Pair("size", size),
					),
				)
			)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedMessage)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(communicationReaderMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should createAlert return 201`() {
		// Arrange
		val createdId = UUID.randomUUID()
		val alert = AlertCreationWriterDto(
			title = "Alert 1",
			dateTime = ZonedDateTime.now(ZoneOffset.UTC),
			message = "test",
			movementId = null
		)

		whenever(service.createAlert(any(), any())).thenReturn(Mono.just(AlertModel()))
		whenever(readerMapper.toDto(any())).thenReturn(AlertReaderDto().apply { id = createdId })
		whenever(creationWriterMapper.toModel(any(), any())).thenReturn(AlertModel())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_C), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(alert)
			.exchange()

		// Assert
		result.expectHeader()
			.valueEquals("Location", "${BASE_URL.replace("{projectId}", defaultProjectId.toString())}/$createdId")
		result.body<AlertReaderDto>(CREATED)

		verify(service).createAlert(any(), any())
		verify(readerMapper).toDto(any())
		verify(creationWriterMapper).toModel(any(), eq(defaultProjectId))
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(communicationReaderMapper)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should createAlert return 400`(
		alert: AlertCreationWriterDto,
		expectedCode: String,
	) {
		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(alert)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedCode)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(communicationReaderMapper)
		verifyNoInteractions(creationWriterMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(service)
	}


	@Test
	fun `Should updateAlert return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val alert = AlertWriterDto(title = "Alert 1", dateTime = ZonedDateTime.now(ZoneOffset.UTC), status = IN_PROGRESS)

		whenever(service.updateAlertById(any(), any(), any(), any())).thenReturn(Mono.just(AlertModel()))
		whenever(readerMapper.toDto(any())).thenReturn(AlertReaderDto())
		whenever(writerMapper.toModel(any(), any())).thenReturn(AlertModel())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_U), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.bodyValue(alert)
			.exchange()

		// Assert
		result.body<AlertReaderDto>(OK)

		verify(service).updateAlertById(any(), eq(defaultProjectId), eq(uuid), any())
		verify(readerMapper).toDto(any())
		verifyNoInteractions(communicationReaderMapper)
		verifyNoInteractions(creationWriterMapper)
		verify(writerMapper).toModel(any(), eq(defaultProjectId))
	}

	@Test
	fun `Should updateAlert accept the matching If-Match`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val alert = AlertWriterDto(title = "Alert 1", dateTime = ZonedDateTime.now(ZoneOffset.UTC), status = IN_PROGRESS)

		whenever(service.updateAlertById(any(), any(), any(), any())).thenReturn(Mono.just(AlertModel()))
		whenever(readerMapper.toDto(any())).thenReturn(AlertReaderDto())
		whenever(writerMapper.toModel(any(), any())).thenReturn(AlertModel())
		whenever(service.findAlertById(any(), any(), anyOrNull())).thenReturn(
			Mono.just(AlertModel().apply {
				lastEdition = HistoryModel(dateTime = ZonedDateTime.parse("2026-10-13T10:02:11Z"))
			}),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_U), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.header("If-Match", "\"1791885731000\"")
			.bodyValue(alert)
			.exchange()

		// Assert
		result.body<AlertReaderDto>(OK)

		verify(service).updateAlertById(any(), eq(defaultProjectId), eq(uuid), any())
		verify(readerMapper).toDto(any())
		verifyNoInteractions(communicationReaderMapper)
		verifyNoInteractions(creationWriterMapper)
		verify(writerMapper).toModel(any(), eq(defaultProjectId))
	}

	@Test
	fun `Should updateAlertById throw when the element changed since it was read`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val alert = AlertWriterDto(title = "Alert 1", dateTime = ZonedDateTime.now(ZoneOffset.UTC), status = IN_PROGRESS)

		whenever(writerMapper.toModel(any(), any())).thenReturn(AlertModel())
		whenever(service.findAlertById(any(), any(), anyOrNull())).thenReturn(
			Mono.just(AlertModel().apply {
				lastEdition = HistoryModel(dateTime = ZonedDateTime.parse("2026-10-13T10:02:11Z"))
			}),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_U), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.header("If-Match", "\"1\"")
			.bodyValue(alert)
			.exchange()

		// Assert
		result.assertError(PRECONDITION_FAILED, RESOURCE_MODIFIED_SINCE_READ)

		verify(service, never()).updateAlertById(any(), eq(defaultProjectId), eq(uuid), any())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should updateAlertById return 400`(
		alert: AlertWriterDto,
		expectedCode: String,
	) {
		// Arrange
		val uuid = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate()
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.bodyValue(alert)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedCode)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(communicationReaderMapper)
		verifyNoInteractions(creationWriterMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should updateAlertStatusById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(service.updateAlertStatusById(any(), any(), any(), any())).thenReturn(Mono.just(AlertModel()))
		whenever(readerMapper.toDto(any())).thenReturn(AlertReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_U), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.patch()
			.uri(
				uriBuilder(
					"$BASE_URL/{id}/status/{status}",
					listOf(defaultProjectId, uuid, IN_PROGRESS),
					emptyList(),
				)
			)
			.exchange()

		// Assert
		result.body<AlertReaderDto>(OK)

		verify(service).updateAlertStatusById(any(), eq(defaultProjectId), eq(uuid), any())
		verify(readerMapper).toDto(any())
		verifyNoInteractions(communicationReaderMapper)
		verifyNoInteractions(creationWriterMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should disableAlertById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(service.disableAlertById(any(), eq(defaultProjectId), eq(uuid))).thenReturn(Mono.just(AlertModel()))
		whenever(readerMapper.toDto(any())).thenReturn(AlertReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_U), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<AlertReaderDto>(OK)

		verify(service).disableAlertById(any(), eq(defaultProjectId), eq(uuid))
		verify(readerMapper).toDto(any())
		verifyNoInteractions(creationWriterMapper)
		verifyNoInteractions(communicationReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should enableAlertById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(service.enableAlertById(any(), eq(defaultProjectId), eq(uuid))).thenReturn(Mono.just(AlertModel()))
		whenever(readerMapper.toDto(any())).thenReturn(AlertReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_D), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<AlertReaderDto>(OK)

		verify(service).enableAlertById(any(), eq(defaultProjectId), eq(uuid))
		verify(readerMapper).toDto(any())
		verifyNoInteractions(communicationReaderMapper)
		verifyNoInteractions(creationWriterMapper)
		verifyNoInteractions(writerMapper)
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
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(NO_CONTENT)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(communicationReaderMapper)
		verifyNoInteractions(creationWriterMapper)
		verifyNoInteractions(writerMapper)
		verify(service).deleteAlertById(any(), eq(defaultProjectId), eq(uuid))
	}
}
