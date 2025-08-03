package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AlertError.ALERT_STATUS_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AlertError.ALERT_TITLE_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AlertError.ALERT_TITLE_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_DATETIME_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MESSAGE_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TYPE_MISMATCH
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_COMMUNICATION_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_U
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ALERT
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum.IN_PROGRESS
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.IAlertService
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.AlertReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.AlertCreationWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.AlertWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.AlertReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.CommunicationReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.AlertCreationWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.AlertWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
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

class AlertControllerTest(@Autowired private val webClient: WebTestClient): TestContext() {
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

    companion object {
        private const val BASE_URL = "/api/projects/{projectId}/alerts"
        private val locale = Locale.ENGLISH

        @JvmStatic
        fun `Should findAlerts prepare param, call service and finally cast the result`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("not locale", null, null, null, null, null, null, null),
                Arguments.of(null, 0, null, null, null, null, null, null),
                Arguments.of(null, null, 200, null, null, null, null, null),
                Arguments.of(null, null, null, null, null, null, null, null),
                Arguments.of(null, null, null, "text", null, null, null, null),
                Arguments.of(null, null, null, null, true, null, null, null),
                Arguments.of(null, null, null, null, null, IN_PROGRESS, null, null),
                Arguments.of(null, null, null, null, null, null, "2024-11-14T18:34:33.000Z", null),
                Arguments.of(null, null, null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
            )
        }

        @JvmStatic
        fun `Should findAlerts throw due to wrong params`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("not uuid", null, null, null, null, null, null, null, PARAMETER_TYPE_MISMATCH),
                Arguments.of(projectId.toString(), - 1, null, null, null, null, null, null, PAGE_NUMBER_IS_LOWER_THAN_ZERO),
                Arguments.of(projectId.toString(), null, 0, null, null, null, null, null, PAGE_SIZE_IS_LOWER_THAN_ONE),
                Arguments.of(projectId.toString(), null, 201, null, null, null, null, null, PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE),
                Arguments.of(projectId.toString(), null, null, null, "not boolean", null, null, null, PARAMETER_TYPE_MISMATCH),
                Arguments.of(projectId.toString(), null, null, null, null, "not boolean", null, null, PARAMETER_TYPE_MISMATCH),
                Arguments.of(projectId.toString(), null, null, null, null, null, "2024/11/14 18:34:33", null, PARAMETER_TYPE_MISMATCH),
                Arguments.of(projectId.toString(), null, null, null, null, null, null, "2024/11/14 18:34:33", PARAMETER_TYPE_MISMATCH),
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
                Arguments.of(- 1, null, PAGE_NUMBER_IS_LOWER_THAN_ZERO),
                Arguments.of(null, 0, PAGE_SIZE_IS_LOWER_THAN_ONE),
                Arguments.of(null, 201, PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE),
            )
        }

        @JvmStatic
        fun `Should createAlert return 400`(): Stream<Arguments> = Stream.of(
            Arguments.of(
                AlertCreationWriterDto(
                    title = null,
                    dateTime = ZonedDateTime.now(),
                    message = null,
                    movementId = null,
                ),
                ALERT_TITLE_NULL_OR_BLANK,
            ),
            Arguments.of(
                AlertCreationWriterDto(
                    title = "Alert name very loooooooooooooooooooooooooooooooooooooooooooooong",
                    dateTime = ZonedDateTime.now(),
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
                    dateTime = ZonedDateTime.now(),
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
                    dateTime = ZonedDateTime.now(),
                    status = IN_PROGRESS,
                ),
                ALERT_TITLE_NULL_OR_BLANK,
            ),
            Arguments.of(
                AlertWriterDto(
                    title = "Alert name very loooooooooooooooooooooooooooooooooooooooooooooong",
                    dateTime = ZonedDateTime.now(),
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
                    dateTime = ZonedDateTime.now(),
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
        pageNumber: Int?,
        pageSize: Int?,
        textSearched: String?,
        visibilitySearched: Boolean?,
        statusSearched: AlertStatusEnum?,
        startDateTimeSearched: String?,
        endDateTimeSearched: String?,
    ) {
        // Arrange
        val expectedPageNumber = pageNumber ?: 0
        val expectedPageSize = pageSize ?: 20
        val pageable = PageableModel(expectedPageNumber * expectedPageSize, expectedPageSize)
        val searchParams = AlertSearchParamModel(
            textSearched = textSearched,
            visibilitySearched = visibilitySearched,
            statusSearched = statusSearched,
            startDateTimeSearched = startDateTimeSearched?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
            endDateTimeSearched = endDateTimeSearched?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
        )
        val page = PageModel(pageable, totalElements = 1, listOf(AlertModel()))
        whenever(service.findAlertsPage(any(), any(), any())).thenReturn(Mono.just(page))
        whenever(readerMapper.toDtoPage(any(), any())).thenReturn(
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
                        Pair("pageNumber", pageNumber),
                        Pair("pageSize", pageSize),
                        Pair("textSearched", textSearched),
                        Pair("visibilitySearched", visibilitySearched),
                        Pair("statusSearched", statusSearched),
                        Pair("startDateTimeSearched", startDateTimeSearched),
                        Pair("endDateTimeSearched", endDateTimeSearched),
                    ),
                )
            )
            .header(ACCEPT_LANGUAGE, requestedLocale)
            .exchange()

        // Assert
        result.body<PageModel<*>>(OK)

        verify(service).findAlertsPage(projectId, pageable, searchParams)
        verify(readerMapper).toDtoPage(page, locale)
        verifyNoInteractions(communicationReaderMapper)
        verifyNoInteractions(writerMapper)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findAlerts throw due to wrong params`(
        alertProjectId: String?,
        pageNumber: Int?,
        pageSize: Int?,
        textSearched: String?,
        visibilitySearched: String?,
        statusSearched: String?,
        startDateTimeSearched: String?,
        endDateTimeSearched: String?,
        expectedMessage: String,
    ) {
        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_R), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
            .get()
            .uri(
                uriBuilder(
                    BASE_URL,
                    if (Objects.nonNull(alertProjectId)) listOf(alertProjectId !!) else emptyList(),
                    listOf(
                        Pair("pageNumber", pageNumber),
                        Pair("pageSize", pageSize),
                        Pair("textSearched", textSearched),
                        Pair("visibilitySearched", visibilitySearched),
                        Pair("statusSearched", statusSearched),
                        Pair("statusSearched", statusSearched),
                        Pair("startDateTimeSearched", startDateTimeSearched),
                        Pair("endDateTimeSearched", endDateTimeSearched),
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
    fun `Should findAlertById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        whenever(service.findAlertById(any(), any(), anyOrNull())).thenReturn(Mono.just(AlertModel()))
        whenever(readerMapper.toDto(any(), any())).thenReturn(AlertReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_R), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
            .get()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<AlertReaderDto>(OK)

        verify(service).findAlertById(projectId, uuid, visibilitySearched = null)
        verify(readerMapper).toDto(any(), any())
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(communicationReaderMapper)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findAlertCommunications prepare param, call service and finally cast the result`(
        pageNumber: Int?,
        pageSize: Int?,
        textSearched: String?,
        visibilitySearched: Boolean?,
        startDateTimeSearched: String?,
        endDateTimeSearched: String?,
    ) {
        // Arrange
        val uuid = UUID.randomUUID()
        val expectedPageNumber = pageNumber ?: 0
        val expectedPageSize = pageSize ?: 20
        val pageable = PageableModel(expectedPageNumber * expectedPageSize, expectedPageSize)
        val searchParams = CommunicationSearchParamModel(
            textSearched = textSearched,
            visibilitySearched = visibilitySearched,
            startDateTimeSearched = startDateTimeSearched?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
            endDateTimeSearched = endDateTimeSearched?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
        )
        val page = PageModel(pageable, totalElements = 1, listOf(CommunicationModel()))
        whenever(service.findAlertCommunicationsPage(any(), any(), any(), any())).thenReturn(Mono.just(page))
        whenever(communicationReaderMapper.toDtoPage(any(), any())).thenReturn(
            PageModel(pageable, totalElements = 1, listOf(CommunicationReaderDto())),
        )

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_COMMUNICATION_R), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
            .get()
            .uri(
                uriBuilder(
                    "$BASE_URL/{id}/communications",
                    listOf(projectId, uuid),
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
        result.body<PageModel<*>>(OK)

        verify(service).findAlertCommunicationsPage(projectId, uuid, pageable, searchParams)
        verify(communicationReaderMapper).toDtoPage(page, locale)
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(writerMapper)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findAlertCommunications throw due to wrong params`(
        pageNumber: Int?,
        pageSize: Int?,
        expectedMessage: String,
    ) {
        // Arrange
        val uuid = UUID.randomUUID()

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_COMMUNICATION_R), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
            .get()
            .uri(
                uriBuilder(
                    "$BASE_URL/{id}/communications",
                    listOf(projectId, uuid),
                    listOf(
                        Pair("pageNumber", pageNumber),
                        Pair("pageSize", pageSize),
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
    fun `Should createAlert return 200`() {
        // Arrange
        val alert = AlertCreationWriterDto(title = "Alert 1", dateTime = ZonedDateTime.now(), message = "test", movementId = null)

        whenever(service.createAlert(any(), any())).thenReturn(Mono.just(AlertModel()))
        whenever(readerMapper.toDto(any(), any())).thenReturn(AlertReaderDto())
        whenever(creationWriterMapper.toModel(any(), any())).thenReturn(AlertModel())

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
        verify(readerMapper).toDto(any(), any())
        verify(creationWriterMapper).toModel(any(), eq(projectId))
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
            .uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
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
        val alert = AlertWriterDto(title = "Alert 1", dateTime = ZonedDateTime.now(), status = IN_PROGRESS)

        whenever(service.updateAlertById(any(), any(), any(), any())).thenReturn(Mono.just(AlertModel()))
        whenever(readerMapper.toDto(any(), any())).thenReturn(AlertReaderDto())
        whenever(writerMapper.toModel(any(), any())).thenReturn(AlertModel())

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
        verify(readerMapper).toDto(any(), any())
        verifyNoInteractions(communicationReaderMapper)
        verifyNoInteractions(creationWriterMapper)
        verify(writerMapper).toModel(any(), eq(projectId))
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
            .uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
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
        whenever(readerMapper.toDto(any(), any())).thenReturn(AlertReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_U), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
            .patch()
            .uri(
                uriBuilder(
                    "$BASE_URL/{id}/status/{status}",
                    listOf(projectId, uuid, IN_PROGRESS),
                    emptyList(),
                )
            )
            .exchange()

        // Assert
        result.body<AlertReaderDto>(OK)

        verify(service).updateAlertStatusById(any(), eq(projectId), eq(uuid), any())
        verify(readerMapper).toDto(any(), any())
        verifyNoInteractions(communicationReaderMapper)
        verifyNoInteractions(creationWriterMapper)
        verifyNoInteractions(writerMapper)
    }

    @Test
    fun `Should disableAlertById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        whenever(service.disableAlertById(any(), eq(projectId), eq(uuid))).thenReturn(Mono.just(AlertModel()))
        whenever(readerMapper.toDto(any(), any())).thenReturn(AlertReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_U), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/disable", listOf(projectId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<AlertReaderDto>(OK)

        verify(service).disableAlertById(any(), eq(projectId), eq(uuid))
        verify(readerMapper).toDto(any(), any())
        verifyNoInteractions(creationWriterMapper)
        verifyNoInteractions(communicationReaderMapper)
        verifyNoInteractions(writerMapper)
    }

    @Test
    fun `Should enableAlertById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        whenever(service.enableAlertById(any(), eq(projectId), eq(uuid))).thenReturn(Mono.just(AlertModel()))
        whenever(readerMapper.toDto(any(), any())).thenReturn(AlertReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_ALERT_U), buildAuthority(REGISTRY_PROJECT_OPTION_ALERT))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/enable", listOf(projectId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<AlertReaderDto>(OK)

        verify(service).enableAlertById(any(), eq(projectId), eq(uuid))
        verify(readerMapper).toDto(any(), any())
        verifyNoInteractions(communicationReaderMapper)
        verifyNoInteractions(creationWriterMapper)
        verifyNoInteractions(writerMapper)
    }

    @Test
    fun `Should deleteAlertById return 200`() {
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
        result.body<Void>(OK)

        verifyNoInteractions(readerMapper)
        verifyNoInteractions(communicationReaderMapper)
        verifyNoInteractions(creationWriterMapper)
        verifyNoInteractions(writerMapper)
        verify(service).deleteAlertById(any(), eq(projectId), eq(uuid))
    }
}
